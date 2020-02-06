package uk.ac.warwick.tabula.web.controllers.attendance.view

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.{JHashMap, JMap}
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.attendance.view.{RecordMonitoringPointCommandState, SetFilterPointsResultOnRecordMonitoringPointCommand, _}
import uk.ac.warwick.tabula.commands.attendance.{AttendanceExtractor, AttendanceExtractorInternal}
import uk.ac.warwick.tabula.commands.{Appliable, FiltersStudentsBase, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringPoint, AttendanceState}
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController

import scala.jdk.CollectionConverters._

@Controller
@RequestMapping(Array("/attendance/view/{department}/{academicYear}/points/{templatePoint}/record/upload"))
class RecordMonitoringPointUploadController extends AttendanceController {

  @ModelAttribute("extractor")
  def extractor = AttendanceExtractor()

  @ModelAttribute("filterCommand")
  def filterCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
    FilterMonitoringPointsCommand(mandatory(department), mandatory(academicYear), user)

  type RecordMonitoringPointCommand = Appliable[Seq[AttendanceMonitoringCheckpoint]] with SetFilterPointsResultOnRecordMonitoringPointCommand
    with SelfValidating with PopulateOnForm with RecordMonitoringPointCommandState

  @ModelAttribute("command")
  def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, @PathVariable templatePoint: AttendanceMonitoringPoint) =
    RecordMonitoringPointCommand(mandatory(department), mandatory(academicYear), mandatory(templatePoint), user)

  @RequestMapping(method = Array(GET))
  def form(
    @ModelAttribute("filterCommand") filterCommand: Appliable[FilterMonitoringPointsCommandResult] with FiltersStudentsBase,
    @ModelAttribute("command") cmd: RecordMonitoringPointCommand,
    errors: Errors,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
    @PathVariable templatePoint: AttendanceMonitoringPoint,
  ): Mav = {
    Mav("attendance/upload_attendance",
      "uploadUrl" -> Routes.View.pointRecordUpload(department, academicYear, templatePoint, filterCommand.serializeFilter),
      "templateUrl" -> Routes.View.pointRecordTemplate(department, academicYear, templatePoint, filterCommand.serializeFilter),
      "ajax" -> ajax,
      "errors" -> errors
    ).crumbs(
      Breadcrumbs.View.HomeForYear(academicYear),
      Breadcrumbs.View.DepartmentForYear(department, academicYear),
      Breadcrumbs.View.Points(department, academicYear)
    ).noLayoutIf(ajax)
  }

  @RequestMapping(method = Array(POST), params = Array("!confirm"))
  def preview(
    @ModelAttribute("extractor") extractor: AttendanceExtractorInternal,
    @ModelAttribute("filterCommand") filterCommand: Appliable[FilterMonitoringPointsCommandResult] with FiltersStudentsBase,
    @ModelAttribute("command") cmd: RecordMonitoringPointCommand,
    errors: Errors,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
    @PathVariable templatePoint: AttendanceMonitoringPoint
  ): Mav = {
    val attendance = extractor.extract(errors)
    if (errors.hasErrors) {
      form(filterCommand, cmd, errors, department, academicYear, templatePoint)
    } else {
      val filterResult = filterCommand.apply()
      cmd.setFilteredPoints(filterResult)
      cmd.populate()
      val existingCheckpointMap = cmd.checkpointMap.asScala
      val newCheckpointMap: JMap[StudentMember, JMap[AttendanceMonitoringPoint, AttendanceState]] =
        JHashMap(cmd.checkpointMap.asScala.map { case (student, pointMap) =>
          student -> JHashMap(pointMap.asScala.map { case (point, oldState) =>
            point -> (attendance.getOrElse(student, oldState) match {
              case state: AttendanceState if state == AttendanceState.NotRecorded => null
              case state => state
            })
          }.toMap)
        }.toMap)
      cmd.checkpointMap = newCheckpointMap
      cmd.validate(errors)
      if (errors.hasErrors) {
        form(filterCommand, cmd, errors, department, academicYear, templatePoint)
      } else {
        val (valid, invalid) = attendance.partition { case (s, _) => cmd.checkpointMap.keySet.contains(s) }
        val (identical, updated) = valid.partition { case (s, a) => existingCheckpointMap.get(s).flatMap(_.asScala.get(templatePoint)) match {
          case Some(null) if a == AttendanceState.NotRecorded => true
          case Some(state) if state == a => true
          case _ => false
        } }
        Mav("attendance/upload_attendance_confirm",
          "uploadUrl" -> Routes.View.pointRecordUpload(department, academicYear, templatePoint, filterCommand.serializeFilter),
          "valid" -> valid.toSeq,
          "updated" -> updated.toSeq.sortBy { case (s, _) => (s.lastName, s.firstName) },
          "identical" -> identical.toSeq.sortBy { case (s, _) => (s.lastName, s.firstName) },
          "invalid" -> invalid.toSeq.sortBy { case (s, _) => (s.lastName, s.firstName) },
        )
      }
    }
  }

  @RequestMapping(method = Array(POST), params = Array("confirm"))
  def confirm(
    @ModelAttribute("filterCommand") filterCommand: Appliable[FilterMonitoringPointsCommandResult] with FiltersStudentsBase,
    @ModelAttribute("command") cmd: RecordMonitoringPointCommand,
    errors: Errors,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
    @PathVariable templatePoint: AttendanceMonitoringPoint,
  ): Mav = {
    val filterResult = filterCommand.apply()
    cmd.setFilteredPoints(filterResult)
    cmd.validate(errors)
    if (errors.hasErrors) {
      form(filterCommand, cmd, errors, department, academicYear, templatePoint)
    } else {
      cmd.apply()
      Redirect(Routes.View.points(department, academicYear))
    }
  }
}
