package uk.ac.warwick.tabula.web.controllers.attendance.old

import uk.ac.warwick.tabula.data.model.{MeetingFormat, MeetingRecord, StudentMember}
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, ModelAttribute}
import uk.ac.warwick.tabula.commands.{Unaudited, ReadOnly, CommandInternal, ComposableCommand, Appliable}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringRelationshipServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.data.{AutowiringMeetingRecordDaoComponent, MeetingRecordDaoComponent}
import scala.collection.mutable
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController

object OldViewMeetingsForPointCommand {
	def apply(student: StudentMember, point: MonitoringPoint) =
		new OldViewMeetingsForPointCommand(student, point)
		with ComposableCommand[Seq[(MeetingRecord, Seq[String])]]
		with OldViewMeetingsForPointPermission
		with OldViewMeetingsForPointCommandState
		with AutowiringRelationshipServiceComponent
		with AutowiringMeetingRecordDaoComponent
		with AutowiringTermServiceComponent
		with ReadOnly with Unaudited
}

class OldViewMeetingsForPointCommand(val student: StudentMember, val point: MonitoringPoint)
	extends CommandInternal[Seq[(MeetingRecord, Seq[String])]] with OldViewMeetingsForPointCommandState {

	self: RelationshipServiceComponent with MeetingRecordDaoComponent with TermServiceComponent =>

	override def applyInternal() = {
		// Get all the enabled relationship types for a department
		val allRelationshipTypes = relationshipService.allStudentRelationshipTypes

		val allMeetings = allRelationshipTypes.flatMap{ relationshipType =>
			relationshipService.getRelationships(relationshipType, student).flatMap(meetingRecordDao.list)
		}

		allMeetings.map{meeting => meeting -> {
			val meetingTermWeek = termService.getAcademicWeekForAcademicYear(meeting.meetingDate, point.pointSet.academicYear)
			val reasons: mutable.Buffer[String] = mutable.Buffer()
			if (!point.meetingRelationships.contains(meeting.relationship.relationshipType))
				reasons += s"Meeting was not with ${point.meetingRelationships.map{_.agentRole}.mkString(" or ")}"

			if (!point.meetingFormats.contains(meeting.format))
				reasons += s"Meeting was not a ${point.meetingFormats.map{_.description}.mkString(" or ")}"

			if (meeting.isRejected)
				reasons += s"Rejected by ${meeting.relationship.relationshipType.agentRole}"
			else if (!meeting.isAttendanceApproved)
				reasons += s"Awaiting approval by ${meeting.relationship.relationshipType.agentRole}"

			if (meetingTermWeek < point.validFromWeek)
				reasons += "Took place before"

			if (meetingTermWeek > point.requiredFromWeek)
				reasons += "Took place after"

			reasons
		}}
	}

}

trait OldViewMeetingsForPointPermission extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: OldViewMeetingsForPointCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.View, student)
	}
}

trait OldViewMeetingsForPointCommandState {
	def student: StudentMember
	def point: MonitoringPoint
}

@Controller
@RequestMapping(Array("/attendance/{department}/{monitoringPoint}/meetings/{student}"))
class OldViewMeetingsForPointController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(@PathVariable student: StudentMember,	@PathVariable monitoringPoint: MonitoringPoint) =
		OldViewMeetingsForPointCommand(student, monitoringPoint)

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[Seq[(MeetingRecord, Seq[String])]]) = {
		val meetingsStatuses = cmd.apply()
		Mav("attendance/home/old/meetings",
			"meetingsStatuses" -> meetingsStatuses,
			"allMeetingFormats" -> MeetingFormat.members
		).noLayoutIf(ajax)
	}
}
