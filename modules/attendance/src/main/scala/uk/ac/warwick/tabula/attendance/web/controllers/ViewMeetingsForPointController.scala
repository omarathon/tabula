package uk.ac.warwick.tabula.attendance.web.controllers

import uk.ac.warwick.tabula.data.model.{MeetingFormat, MeetingRecord, StudentMember}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, ModelAttribute}
import uk.ac.warwick.tabula.commands.{Unaudited, ReadOnly, CommandInternal, ComposableCommand, Appliable}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermServiceComponent, AutowiringRelationshipServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.data.{AutowiringMeetingRecordDaoComponent, MeetingRecordDaoComponent}
import scala.collection.mutable
import org.springframework.stereotype.Controller

object ViewMeetingsForPointCommand {
	def apply(student: StudentMember, point: MonitoringPoint) =
		new ViewMeetingsForPointCommand(student, point)
		with ComposableCommand[Seq[Pair[MeetingRecord, Seq[String]]]]
		with ViewMeetingsForPointPermission
		with ViewMeetingsForPointCommandState
		with AutowiringRelationshipServiceComponent
		with AutowiringMeetingRecordDaoComponent
		with AutowiringTermServiceComponent
		with ReadOnly with Unaudited
}

class ViewMeetingsForPointCommand(val student: StudentMember, val point: MonitoringPoint)
	extends CommandInternal[Seq[Pair[MeetingRecord, Seq[String]]]] with ViewMeetingsForPointCommandState {

	self: RelationshipServiceComponent with MeetingRecordDaoComponent with TermServiceComponent =>

	override def applyInternal() = {
		// Get all the enabled relationship types for a department
		val allRelationshipTypes = relationshipService.allStudentRelationshipTypes

		val allMeetings = allRelationshipTypes.flatMap{ relationshipType =>
			student.mostSignificantCourseDetails.map{ scd =>
				relationshipService.getRelationships(relationshipType, scd.sprCode).flatMap(meetingRecordDao.list(_))
			}
		}.flatten

		allMeetings.map{meeting => meeting -> {
			val meetingTermWeek = termService.getAcademicWeekForAcademicYear(meeting.meetingDate, point.pointSet.asInstanceOf[MonitoringPointSet].academicYear)
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

trait ViewMeetingsForPointPermission extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewMeetingsForPointCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.View, student)
	}
}

trait ViewMeetingsForPointCommandState {
	def student: StudentMember
	def point: MonitoringPoint
}

@Controller
@RequestMapping(Array("/{department}/{monitoringPoint}/meetings/{student}"))
class ViewMeetingsForPointController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(@PathVariable student: StudentMember,	@PathVariable monitoringPoint: MonitoringPoint) =
		ViewMeetingsForPointCommand(student, monitoringPoint)

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[Seq[Pair[MeetingRecord, Seq[String]]]]) = {
		val meetingsStatuses = cmd.apply()
		Mav("home/meetings",
			"meetingsStatuses" -> meetingsStatuses,
			"allMeetingFormats" -> MeetingFormat.members
		).noLayoutIf(ajax)
	}
}
