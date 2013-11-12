package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.{Member, StudentMember, MeetingFormat, StudentRelationshipType, StudentCourseDetails}
import org.joda.time.{LocalDate, DateTime}
import uk.ac.warwick.tabula.commands.{Appliable, Unaudited, ReadOnly, CommandInternal, ComposableCommand}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointMeetingRelationshipTermServiceComponent, MonitoringPointMeetingRelationshipTermServiceComponent}
import uk.ac.warwick.tabula.web.views.JSONView

object MeetingCheckpointCommand {
	def apply(student: StudentMember, relationshipType: StudentRelationshipType, meetingFormat: MeetingFormat, meetingDate: DateTime, currentMember: Member) =
		new MeetingCheckpointCommand(student, relationshipType, meetingFormat, meetingDate, currentMember)
		with ComposableCommand[Boolean]
		with MeetingCheckpointCommandPermissions
		with MeetingCheckpointCommandState
		with AutowiringMonitoringPointMeetingRelationshipTermServiceComponent
		with ReadOnly with Unaudited
}

class MeetingCheckpointCommand(
	val student: StudentMember, val relationshipType: StudentRelationshipType, val meetingFormat: MeetingFormat, val meetingDate: DateTime, val currentMember: Member
) extends CommandInternal[Boolean] {

	self: MonitoringPointMeetingRelationshipTermServiceComponent =>

	def applyInternal() = {
		monitoringPointMeetingRelationshipTermService.willCheckpointBeCreated(student, relationshipType, meetingFormat, meetingDate, None)
	}

}

trait MeetingCheckpointCommandPermissions extends RequiresPermissionsChecking {
	this: MeetingCheckpointCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(p.mandatory(relationshipType)), currentMember)
	}
}

trait MeetingCheckpointCommandState {
	def student: StudentMember
	def relationshipType: StudentRelationshipType
	def meetingFormat: MeetingFormat
	def meetingDate: DateTime
	def currentMember: Member
}

@Controller
@RequestMapping(Array("/profile/{studentCourseDetails}/meetingcheckpoint"))
class MeetingCheckpointController extends AttendanceController {

	@ModelAttribute("command")
	def command(
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@RequestParam(value="relationshipType", required = false) relationshipType: StudentRelationshipType,
		@RequestParam(value="meetingFormat", required = false) meetingFormat: MeetingFormat,
		@RequestParam(value="meetingDate", required = false) meetingDate: LocalDate
	) =
		MeetingCheckpointCommand(
			studentCourseDetails.student,
			mandatory(relationshipType),
			mandatory(meetingFormat),
			mandatory(meetingDate).toDateTimeAtStartOfDay,
			currentMember
		)

	@RequestMapping
	def render(@ModelAttribute("command") cmd: Appliable[Boolean]) =
		Mav(new JSONView(Map("willCheckpointBeCreated" -> cmd.apply())))
}
