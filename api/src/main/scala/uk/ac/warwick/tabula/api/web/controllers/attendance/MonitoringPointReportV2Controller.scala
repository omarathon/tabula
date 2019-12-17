package uk.ac.warwick.tabula.api.web.controllers.attendance

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestBody, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.api.commands.JsonApiRequest
import uk.ac.warwick.tabula.api.commands.attendance.{SynchroniseAttendanceToSitsBySequenceCommand, SynchroniseAttendanceToSitsBySequenceRequest}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, Features, SprCode}

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._

@Controller
@RequestMapping(Array("/v2/department/{department}/monitoring-point-reports"))
class MonitoringPointReportV2Controller extends ApiController
  with MonitoringPointReportV2CreateApi

// POST or PATCH - Synchronise report
trait MonitoringPointReportV2CreateApi {
  self: ApiController =>

  @ModelAttribute("syncCommand")
  def command(@PathVariable department: Department, user: CurrentUser): SynchroniseAttendanceToSitsBySequenceCommand.Command =
    SynchroniseAttendanceToSitsBySequenceCommand(department, user)

  @RequestMapping(method = Array(POST, PATCH), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
  def sync(@RequestBody request: CreateMonitoringPointReportV2Request, @ModelAttribute("syncCommand") command: SynchroniseAttendanceToSitsBySequenceCommand.Command, errors: Errors): Mav = {
    request.copyTo(command, errors)

    globalValidator.validate(command, errors)
    command.validate(errors)

    if (errors.hasErrors) {
      Mav(new JSONErrorView(errors))
    } else {
      val result = command.apply()
      Mav(new JSONView(Map(
        "success" -> true,
        "status" -> "ok",
        "academicYear" -> request.academicYear,
        "missedPoints" -> result.map { report =>
          report.student.universityId -> report.missedPoints
        }.toMap
      )))
    }
  }
}

@JsonAutoDetect
class CreateMonitoringPointReportV2Request extends JsonApiRequest[SynchroniseAttendanceToSitsBySequenceRequest] {
  @transient var profileService: ProfileService = Wire[ProfileService]
  @transient var features: Features = Wire[Features]

  @BeanProperty var academicYear: AcademicYear = _
  @BeanProperty var missedPoints: JMap[String, JInteger] = JHashMap()

  override def copyTo(state: SynchroniseAttendanceToSitsBySequenceRequest, errors: Errors) {
    if (!features.attendanceMonitoringRealTimeReport) {
      errors.reject("attendanceMonitoringReport.realTimeNotEnabled")
    }

    state.academicYear = academicYear
    state.missedPoints = missedPoints.asScala.flatMap { case (sprCode, missed) =>
      profileService.getMemberByUniversityId(SprCode.getUniversityId(sprCode)) match {
        case Some(student: StudentMember) =>
          Some(student -> missed.intValue())
        case _ =>
          errors.rejectValue("missedPoints", "monitoringPointReport.student.notFound", Array(sprCode), "")
          None
      }
    }.toMap
  }
}
