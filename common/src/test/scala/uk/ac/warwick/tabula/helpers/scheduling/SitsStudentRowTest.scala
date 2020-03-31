package uk.ac.warwick.tabula.helpers.scheduling

import org.joda.time.{DateTimeConstants, LocalDate}
import uk.ac.warwick.tabula.{AcademicYear, TestBase}
import uk.ac.warwick.tabula.sandbox.MapResultSet

class SitsStudentRowTest extends TestBase {

  @Test def ordering(): Unit = {
    val baseProperties: Map[String, Any] = Map(
      "university_id" -> "1234567",
      "title" -> "MR",
      "preferred_forename" -> "TEST",
      "forenames" -> "TESTUAL WONDERFUL",
      "family_name" -> "DATA",
      "gender" -> "M",
      "email_address" -> "T.W.Data@warwick.ac.uk",
      "user_code" -> "u1234567",
      "date_of_birth" -> new LocalDate(1994, DateTimeConstants.JULY, 19).toDateTimeAtStartOfDay,
      "alternative_email_address" -> "testytesttest@gmail.com",
      "mobile_number" -> "07000123456",
      "nationality" -> "British (ex. Channel Islands & Isle of Man)",
      "second_nationality" -> null,
      "tier4_visa_requirement" -> 0,
      "disability" -> "A",
      "disabilityFunding" -> null,
      "mst_type" -> "L"
    )

    val course1Properties: Map[String, Any] = Map(
      "in_use_flag" -> "Inactive",
      "course_code" -> "TEST-1",
      "course_year_length" -> "4",
      "spr_code" -> "1234567/1",
      "route_code" -> "T100",
      "department_code" -> "TS",
      "award_code" -> "BSC",
      "spr_status_code" -> "P",
      "scj_status_code" -> "P",
      "level_code" -> "4",
      "spr_tutor1" -> null,
      "spr_academic_year_start" -> AcademicYear.starting(2014).toString,
      "scj_tutor1" -> null,
      "scj_transfer_reason_code" -> "SC",
      "scj_code" -> "1234567/1",
      "begin_date" -> new LocalDate(2014, DateTimeConstants.SEPTEMBER, 29).toDateTimeAtStartOfDay,
      "end_date" -> new LocalDate(2018, DateTimeConstants.JUNE, 30).toDateTimeAtStartOfDay,
      "expected_end_date" -> new LocalDate(2018, DateTimeConstants.JUNE, 30).toDateTimeAtStartOfDay,
      "most_signif_indicator" -> null,
      "funding_source" -> null,
      "special_exam_arrangements" -> null,
      "special_exam_arrangements_room_code" -> null,
      "special_exam_arrangements_room_name" -> null,
      "special_exam_arrangements_extra_time" -> null,
    )

    val row1 = SitsStudentRow(new MapResultSet(baseProperties ++ course1Properties ++ Map(
      "enrolment_status_code" -> "F",
      "study_block" -> 3,
      "study_level" -> "3",
      "mode_of_attendance_code" -> "F",
      "block_occurrence" -> "F",
      "sce_academic_year" -> AcademicYear.starting(2016).toString,
      "sce_sequence_number" -> 3,
      "sce_route_code" -> "T105",
      "enrolment_department_code" -> "TS",
      "mod_reg_status" -> "CON",
      "sce_agreed_mark" -> null
    )))

    val row2 = SitsStudentRow(new MapResultSet(baseProperties ++ course1Properties ++ Map(
      "enrolment_status_code" -> "P",
      "study_block" -> 4,
      "study_level" -> "4",
      "mode_of_attendance_code" -> "F",
      "block_occurrence" -> "F",
      "sce_academic_year" -> AcademicYear.starting(2017).toString,
      "sce_sequence_number" -> 4,
      "sce_route_code" -> "T100",
      "enrolment_department_code" -> "TS",
      "mod_reg_status" -> "CON",
      "sce_agreed_mark" -> null
    )))

    val course2Properties: Map[String, Any] = Map(
      "in_use_flag" -> "Active",
      "course_code" -> "XXX-2",
      "course_year_length" -> "3",
      "spr_code" -> "1234567/2",
      "route_code" -> "X456",
      "department_code" -> "XX",
      "award_code" -> "PHD",
      "spr_status_code" -> "C",
      "scj_status_code" -> "C",
      "level_code" -> "M2",
      "spr_tutor1" -> "9876543",
      "spr_academic_year_start" -> AcademicYear.starting(2018).toString,
      "scj_tutor1" -> "WM9876543",
      "scj_transfer_reason_code" -> null,
      "scj_code" -> "1234567/2",
      "begin_date" -> new LocalDate(2018, DateTimeConstants.OCTOBER, 1).toDateTimeAtStartOfDay,
      "end_date" -> null,
      "expected_end_date" -> new LocalDate(2022, DateTimeConstants.OCTOBER, 1).toDateTimeAtStartOfDay,
      "most_signif_indicator" -> "Y",
      "funding_source" -> null,
      "special_exam_arrangements" -> "Y",
      "special_exam_arrangements_room_code" -> "INDEPT",
      "special_exam_arrangements_room_name" -> "In department",
      "special_exam_arrangements_extra_time" -> "30",
    )

    val row3 = SitsStudentRow(new MapResultSet(baseProperties ++ course2Properties ++ Map(
      "enrolment_status_code" -> "F",
      "study_block" -> 1,
      "study_level" -> "M2",
      "mode_of_attendance_code" -> "F",
      "block_occurrence" -> "FW",
      "sce_academic_year" -> AcademicYear.starting(2018).toString,
      "sce_sequence_number" -> 1,
      "sce_route_code" -> "X456",
      "enrolment_department_code" -> "XX",
      "mod_reg_status" -> null,
      "sce_agreed_mark" -> null
    )))

    Seq(row3, row1, row2).sorted should be (Seq(row1, row2, row3))
  }

}
