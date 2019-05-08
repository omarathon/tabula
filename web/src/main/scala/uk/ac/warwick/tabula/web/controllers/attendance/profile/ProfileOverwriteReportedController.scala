package uk.ac.warwick.tabula.web.controllers.attendance.profile

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, PostMapping, RequestMapping}
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.attendance.StudentOverwriteReportedCommand
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringNote, AttendanceMonitoringPoint, MonitoringPointReport}
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringServiceComponent
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.web.{Mav, Routes}

@Controller
@RequestMapping(Array("/attendance/profile/{student}/overwrite/{point}"))
class ProfileOverwriteReportedController
  extends AttendanceController
    with AutowiringAttendanceMonitoringServiceComponent {

  validatesSelf[SelfValidating]

  @ModelAttribute("command")
  def command(@PathVariable student: StudentMember, @PathVariable point: AttendanceMonitoringPoint): StudentOverwriteReportedCommand.Command =
    StudentOverwriteReportedCommand(mandatory(student), mandatory(point), user)

  @ModelAttribute("department")
  def department(@PathVariable student: StudentMember): Department = student.homeDepartment

  @ModelAttribute("checkpoint")
  def checkpoint(@PathVariable student: StudentMember, @PathVariable point: AttendanceMonitoringPoint): Option[AttendanceMonitoringCheckpoint] =
    attendanceMonitoringService.getCheckpoints(Seq(point), student).values.headOption

  @ModelAttribute("monitoringPointReport")
  def monitoringPointReport(@PathVariable student: StudentMember, @PathVariable point: AttendanceMonitoringPoint): Option[MonitoringPointReport] =
    attendanceMonitoringService.findReports(Seq(student.universityId), point.scheme.academicYear, point.scheme.academicYear.termOrVacationForDate(point.startDate).periodType.toString)
      .headOption

  @ModelAttribute("note")
  def attendanceNote(@PathVariable student: StudentMember, @PathVariable point: AttendanceMonitoringPoint): Option[AttendanceMonitoringNote] =
    attendanceMonitoringService.getAttendanceNote(student, point)

  @RequestMapping
  def form(@ModelAttribute("command") command: StudentOverwriteReportedCommand.Command, @PathVariable student: StudentMember, @PathVariable point: AttendanceMonitoringPoint): Mav = {
    command.populate()
    renderForm(student, point)
  }

  private def renderForm(student: StudentMember, point: AttendanceMonitoringPoint): Mav =
    Mav("attendance/overwrite-reported")
      .crumbs(Breadcrumbs.Profile.ProfileForYear(student, point.scheme.academicYear))

  @PostMapping
  def record(@Valid @ModelAttribute("command") command: StudentOverwriteReportedCommand.Command, errors: Errors, @PathVariable student: StudentMember, @PathVariable point: AttendanceMonitoringPoint): Mav =
    if (errors.hasErrors) renderForm(student, point)
    else {
      command.apply()
      RedirectForce(Routes.attendance.Profile.profileForYear(student, point.scheme.academicYear))
    }

}
