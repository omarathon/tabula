package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.web.bind.annotation.{RequestBody, PathVariable, ModelAttribute, RequestMapping}
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{StudentMember, Department}
import uk.ac.warwick.tabula.attendance.commands.report.{CreateMonitoringPointReportCommandState, CreateMonitoringPointReportCommand, CreateMonitoringPointReportRequestState}
import uk.ac.warwick.tabula.{SprCode, AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointReport
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}
import javax.servlet.http.HttpServletResponse
import org.springframework.http.{MediaType, HttpStatus}
import org.codehaus.jackson.annotate.JsonAutoDetect
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.services.ProfileService
import scala.collection.JavaConverters._
import uk.ac.warwick.spring.Wire
import scala.beans.BeanProperty

@Controller
@RequestMapping(Array("/report/{department}/create"))
class CreateMonitoringPointReportController extends AttendanceController {

	type CreateMonitoringPointReportCommand = Appliable[Seq[MonitoringPointReport]] with CreateMonitoringPointReportCommandState with SelfValidating

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department, user: CurrentUser) =
		CreateMonitoringPointReportCommand(department, user)

	@RequestMapping(method = Array(POST), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
	def createAsJson(@RequestBody request: CreateMonitoringPointReportRequest, @ModelAttribute("command") command: CreateMonitoringPointReportCommand, errors: Errors)(implicit response: HttpServletResponse) = {
		request.copyTo(command, errors)
		command.validate(errors)

		if (errors.hasErrors) {
			response.setStatus(HttpStatus.BAD_REQUEST.value())

			Mav(new JSONErrorView(errors, Map("success" -> false, "status" -> HttpStatus.BAD_REQUEST.value())))
		} else {
			Mav(new JSONView(Map(
				"success" -> true,
				"academicYear" -> command.academicYear.toString,
				"period" -> command.period,
				"missedPoints" -> command.apply().map { report => (report.student.universityId) -> report.missed }.toMap
			)))
		}
	}

}

@JsonAutoDetect
class CreateMonitoringPointReportRequest extends Serializable {
	@transient var profileService = Wire[ProfileService]

	@BeanProperty var period: String = _
	@BeanProperty var academicYear: String = _
	@BeanProperty var missedPoints: JMap[String, JInteger] = JHashMap()

	def copyTo(state: CreateMonitoringPointReportRequestState, errors: Errors) {
		try {
			state.academicYear = AcademicYear.parse(academicYear)
		} catch {
			case e: IllegalArgumentException => errors.rejectValue("academicYear", "typeMismatch")
		}

		state.missedPoints = missedPoints.asScala.flatMap { case (sprCode, missed) =>
			profileService.getMemberByUniversityId(SprCode.getUniversityId(sprCode)) match {
				case Some(student: StudentMember) => Some(student -> missed.intValue())
				case _ => errors.rejectValue("missedPoints", "invalid"); None
			}
		}.toMap

		state.period = period
	}
}
