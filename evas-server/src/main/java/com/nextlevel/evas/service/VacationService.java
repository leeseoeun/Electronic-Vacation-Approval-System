package com.nextlevel.evas.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.nextlevel.evas.domain.Vacation;
import com.nextlevel.evas.domain.VacationDate;
import com.nextlevel.evas.form.VacationApplicationForm;
import com.nextlevel.evas.form.VacationCancellationForm;
import com.nextlevel.evas.repository.VacationRepository;

@Service
public class VacationService {

  private final VacationRepository vacationRepository;

  @Autowired
  public VacationService(VacationRepository vacationRepository) {
    this.vacationRepository = vacationRepository;
  }

  // main 접속, 새로 고침 시
  public Map<String, List<Vacation>> findAllList(String employeeId) {

    // 연차 사용 상태 업데이트
    vacationRepository.updateUseStatus();
    vacationRepository.updateWholeUseStatus();

    Map<String, List<Vacation>> result = new HashMap<String, List<Vacation>>();

    result.put("applicationList", vacationRepository.findAllApplicationByEmployeeId(employeeId));
    result.put("vacationList", vacationRepository.findAllVacationByEmployeeId(employeeId));
    result.put("calendarList", vacationRepository.findAllCalendar());

    return result;
  }

  // 연차 신청, 수정 시
  public Vacation apply(VacationApplicationForm form) {
    Vacation vacation = new Vacation();

    vacation.setCode(form.getCode());
    vacation.setStart(parseStringToDate(form.getStart()));
    vacation.setEnd(parseStringToDate(form.getEnd()));
    vacation.setContent(form.getContent());
    vacation.setEmployeeId(form.getEmployeeId());

    int result = 0;
    String type = "";
    // 수정
    if (form.getIdx() != null) {
      type = "U";

      vacation.setIdx(Integer.parseInt(form.getIdx()));
      result = vacationRepository.update(vacation);

      // 신청
    } else {
      type = "A";

      result = vacationRepository.insert(vacation);
    }

    if (result > 0) {
      return setVacationDate(vacation.getIdx(), form.getDate(), type);
    } else {
      return null;
    }
  }

  // 연차 삭제 시
  public int delete(String idx) {
    int result = vacationRepository.deleteDate(Integer.parseInt(idx));
    if (result > 0) {
      result = 0;
      result = vacationRepository.delete(Integer.parseInt(idx));
      if (result > 0) {
        return Integer.parseInt(idx);
      } else {
        return -1;
      }
    } else {
      return -1;
    }
  }

  // 연차 취소 시
  public Vacation cancel(VacationCancellationForm form) {
    int result = vacationRepository.updateCancellationContent(Integer.parseInt(form.getIdx()), form.getCancellationContent());
    if (result > 0) {
      return vacationRepository.findByIdx(Integer.parseInt(form.getIdx()));
    } else {
      return null;
    }
  }

  private LocalDate parseStringToDate(String str) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    LocalDate date = LocalDate.parse(str, formatter);

    return date;
  }

  private List<VacationDate> createVacationDateList(int idx, List<String> strList) {
    List<VacationDate> vacationDateList = new ArrayList<>();

    for (String date : strList) {
      VacationDate vacationDate = new VacationDate();

      vacationDate.setVacationIdx(idx);
      vacationDate.setDate(parseStringToDate(date));

      vacationDateList.add(vacationDate);
    }

    return vacationDateList;
  }

  private Vacation setVacationDate(int idx, List<String> vacationDate, String type) {
    List<VacationDate> vacationDateList = createVacationDateList(idx, vacationDate);

    int result = 0;
    if (type.equals("U")) {
      result = vacationRepository.deleteDate(idx);
    }

    if ((type.equals("U") && result > 0) || type.equals("A")) {
      result = 0;
      result = vacationRepository.insertDate(vacationDateList);
    } else {
      return null;
    }

    if (result > 0) {
      return vacationRepository.findByIdx(idx);
    } else {
      return null;
    }
  }

}
