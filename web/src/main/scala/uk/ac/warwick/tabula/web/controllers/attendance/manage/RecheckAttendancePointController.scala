package uk.ac.warwick.tabula.web.controllers.attendance.manage

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.attendance.manage.{FindPointsCommand, FindPointsResult, RecheckAttendanceCommand, SetsFindPointsResultOnCommandState}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.attendance.{AttendanceController, HasMonthNames}

@Controller
@RequestMapping(Array("/attendance/manage/{department}/{academicYear}/editpoints/{templatePoint}/recheck"))
class RecheckAttendancePointController extends AttendanceController with HasMonthNames {

  @ModelAttribute("findCommand")
  def findCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
    FindPointsCommand(mandatory(department), mandatory(academicYear), None)

  @ModelAttribute("command")
  def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, @PathVariable templatePoint: AttendanceMonitoringPoint) =
    RecheckAttendanceCommand(mandatory(department), mandatory(academicYear), mandatory(templatePoint), user)

  @GetMapping
  def form(
    @ModelAttribute("findCommand") findCommand: Appliable[FindPointsResult],
    @ModelAttribute("command") cmd: Appliable[Unit] with SetsFindPointsResultOnCommandState,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
    @PathVariable templatePoint: AttendanceMonitoringPoint
  ): Mav = {
    cmd.setFindPointsResult(findCommand.apply())
    render(cmd, department, academicYear)
  }

  @PostMapping
  def post(
    @ModelAttribute("findCommand") findCommand: Appliable[FindPointsResult],
    @ModelAttribute("command") cmd: Appliable[Unit] with SetsFindPointsResultOnCommandState,
    errors: Errors,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
    @PathVariable templatePoint: AttendanceMonitoringPoint
  ): Mav = {
    cmd.setFindPointsResult(findCommand.apply())
    if (errors.hasErrors) {
      render(cmd, department, academicYear)
    } else {
      cmd.apply()
      Redirect(Routes.View.points(department, academicYear))
    }
  }

  private def render(cmd: Appliable[Unit] with SetsFindPointsResultOnCommandState, department: Department, academicYear: AcademicYear) = {
    Mav("attendance/recheck",
      "command" -> cmd,
      "returnTo" -> getReturnTo(Routes.Manage.editPoints(department, academicYear))
    ).crumbs(
      Breadcrumbs.Manage.HomeForYear(academicYear),
      Breadcrumbs.Manage.DepartmentForYear(department, academicYear),
      Breadcrumbs.Manage.EditPoints(department, academicYear)
    )
  }
}