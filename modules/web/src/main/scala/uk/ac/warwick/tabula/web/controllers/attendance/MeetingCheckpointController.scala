package uk.ac.warwick.tabula.web.controllers.attendance

import org.joda.time.{DateTime, LocalDate}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.commands.{Appliable, CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.{MeetingFormat, StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointMeetingRelationshipTermServiceComponent, MonitoringPointMeetingRelationshipTermServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.views.JSONView

object MeetingCheckpointCommand {
	def apply(student: StudentMember, relationshipType: StudentRelationshipType, meetingFormat: MeetingFormat, meetingDate: DateTime) =
		new MeetingCheckpointCommand(student, relationshipType, meetingFormat, meetingDate)
		with ComposableCommand[Boolean]
		with MeetingCheckpointCommandPermissions
		with MeetingCheckpointCommandState
		with AutowiringMonitoringPointMeetingRelationshipTermServiceComponent
		with ReadOnly with Unaudited
}

class MeetingCheckpointCommand(
	val student: StudentMember, val relationshipType: StudentRelationshipType, val meetingFormat: MeetingFormat, val meetingDate: DateTime
) extends CommandInternal[Boolean] {

	self: MonitoringPointMeetingRelationshipTermServiceComponent =>

	def applyInternal() = {
		monitoringPointMeetingRelationshipTermService.willCheckpointBeCreated(student, relationshipType, meetingFormat, meetingDate, None)
	}

}

trait MeetingCheckpointCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	this: MeetingCheckpointCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.MeetingRecord.Read(mandatory(relationshipType)), student)
	}
}

trait MeetingCheckpointCommandState {
	def student: StudentMember
	def relationshipType: StudentRelationshipType
	def meetingFormat: MeetingFormat
	def meetingDate: DateTime
}

@Controller
@RequestMapping(Array("/attendance/profile/{student}/meetingcheckpoint"))
class MeetingCheckpointController extends AttendanceController {

	@ModelAttribute("command")
	def command(
		@PathVariable student: StudentMember,
		@RequestParam(value="relationshipType", required = false) relationshipType: StudentRelationshipType,
		@RequestParam(value="meetingFormat", required = false) meetingFormat: MeetingFormat,
		@RequestParam(value="meetingDate", required = false) meetingDate: LocalDate
	) =
		MeetingCheckpointCommand(
			student,
			mandatory(relationshipType),
			mandatory(meetingFormat),
			mandatory(meetingDate).toDateTimeAtStartOfDay
		)

	@RequestMapping
	def render(@ModelAttribute("command") cmd: Appliable[Boolean]) =
		Mav(new JSONView(Map("willCheckpointBeCreated" -> cmd.apply())))
}
