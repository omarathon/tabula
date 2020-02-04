package uk.ac.warwick.tabula.web.controllers.reports.smallgroups

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.reports.smallgroups.AllSmallGroupEventAttendanceReportCommand.Result
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.reports.web.Routes

@Controller
@RequestMapping(Array("/reports/{department}/{academicYear}/groups/attendance/all"))
class AllSmallGroupEventAttendanceReportController extends AbstractSmallGroupEventAttendanceReportController {

  val pageRenderPath = "all"
  val filePrefix = "all-small-group-attendance-registers"

  override def urlGeneratorFactory(department: Department): AcademicYear => String =
    year => Routes.SmallGroups.AttendanceRegisters.all(department, year)

  override def filterResults(result: Result): Result = result
}

@Controller
@RequestMapping(Array("/reports/{department}/{academicYear}/groups/attendance/unrecorded"))
class UnrecordedSmallGroupEventAttendanceReportController extends AbstractSmallGroupEventAttendanceReportController {

  val pageRenderPath = "unrecorded"
  val filePrefix = "unrecorded-small-group-attendance-registers"

  override def urlGeneratorFactory(department: Department): AcademicYear => String =
    year => Routes.SmallGroups.AttendanceRegisters.unrecorded(department, year)

  override def filterResults(result: Result): Result = result.filter(_.unrecorded > 0)

}

@Controller
@RequestMapping(Array("/reports/{department}/{academicYear}/groups/attendance/recorded-late"))
class RecordedLateSmallGroupEventAttendanceReportController extends AbstractSmallGroupEventAttendanceReportController {

  val pageRenderPath = "recorded-late"
  val filePrefix = "recorded-late-small-group-attendance-registers"

  override def urlGeneratorFactory(department: Department): AcademicYear => String =
    year => Routes.SmallGroups.AttendanceRegisters.recordedLate(department, year)

  override def filterResults(result: Result): Result = result.filter(r => r.latestRecordedAttendance.exists(_.isAfter(r.eventWeek.date)))

}
