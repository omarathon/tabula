package uk.ac.warwick.tabula.web.controllers.attendance.agent

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, PostMapping, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.attendance.agent.{AgentPointRecordCommand, AgentPointRecordCommandState}
import uk.ac.warwick.tabula.commands.attendance.{AttendanceExtractor, AttendanceExtractorInternal}
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringCheckpointTotal, AttendanceMonitoringPoint, AttendanceState}
import uk.ac.warwick.tabula.data.model.{StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.attendance.{AttendanceController, HasMonthNames}

import scala.jdk.CollectionConverters._

@Controller
@RequestMapping(Array("/attendance/agent/{relationshipType}/{academicYear}/point/{templatePoint}/upload"))
class AgentPointRecordUploadController extends AttendanceController with HasMonthNames {

  type AgentPointRecordCommand = Appliable[(Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])] with SelfValidating
    with AgentPointRecordCommandState with PopulateOnForm

  @ModelAttribute("extractor")
  def extractor: AttendanceExtractorInternal = AttendanceExtractor()

  @ModelAttribute("command")
  def command(
    @PathVariable relationshipType: StudentRelationshipType,
    @PathVariable academicYear: AcademicYear,
    @PathVariable templatePoint: AttendanceMonitoringPoint
  ): AgentPointRecordCommand =
    AgentPointRecordCommand(mandatory(relationshipType), mandatory(academicYear), mandatory(templatePoint), user, currentMember)

  @RequestMapping
  def form(
    @PathVariable relationshipType: StudentRelationshipType,
    @PathVariable academicYear: AcademicYear,
    @PathVariable templatePoint: AttendanceMonitoringPoint
  ): Mav = {
    Mav("attendance/upload_attendance",
      "uploadUrl" -> Routes.Agent.pointRecordUpload(relationshipType, academicYear, templatePoint),
      "ajax" -> ajax
    ).crumbs(
      Breadcrumbs.Agent.RelationshipForYear(relationshipType, academicYear)
    ).noLayoutIf(ajax)
  }

  @PostMapping
  def post(
    @ModelAttribute("extractor") extractor: AttendanceExtractorInternal,
    @ModelAttribute("command") cmd: AgentPointRecordCommand,
    errors: Errors,
    @PathVariable relationshipType: StudentRelationshipType,
    @PathVariable academicYear: AcademicYear,
    @PathVariable templatePoint: AttendanceMonitoringPoint
  ): Mav = {
    val attendance = extractor.extract(errors)
    if (errors.hasErrors) {
      form(relationshipType, academicYear, templatePoint)
    } else {
      cmd.populate()
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
        form(relationshipType, academicYear, templatePoint)
      } else {
        cmd.apply()
        Redirect(Routes.Agent.relationshipForYear(relationshipType, academicYear))
      }
    }
  }

}
