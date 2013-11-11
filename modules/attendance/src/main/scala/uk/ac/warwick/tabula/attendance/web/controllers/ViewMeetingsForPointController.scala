package uk.ac.warwick.tabula.attendance.web.controllers

import uk.ac.warwick.tabula.data.model.{MeetingRecord, StudentMember}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, ModelAttribute}
import uk.ac.warwick.tabula.commands.{Unaudited, ReadOnly, CommandInternal, ComposableCommand, Appliable}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermServiceComponent, AutowiringRelationshipServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.data.{AutowiringMeetingRecordDaoComponent, MeetingRecordDaoComponent}
import scala.collection.mutable
import org.springframework.stereotype.Controller
import scala.collection.JavaConverters._

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
		val allRelationshipTypes =
			Option(student.homeDepartment)
				.map { _.displayedStudentRelationshipTypes }
				.getOrElse { relationshipService.allStudentRelationshipTypes }

		val allMeetings = allRelationshipTypes.flatMap{ relationshipType =>
			student.freshStudentCourseDetails.flatMap{ scd =>
				relationshipService.getRelationships(relationshipType, scd.sprCode).flatMap(meetingRecordDao.list(_))
			}
		}
		allMeetings.map{meeting => meeting -> {
			val reasons: mutable.Buffer[String] = mutable.Buffer()
			if (!point.meetingRelationships.contains(meeting.relationship.relationshipType))
				reasons += "Incorrect relationship"
			if (!point.meetingFormats.contains(meeting.format))
				reasons += "Incorrect format"
			if (!meeting.isAttendanceApproved)
				reasons += "Not approved"
			if (termService.getAcademicWeekForAcademicYear(meeting.meetingDate, point.pointSet.asInstanceOf[MonitoringPointSet].academicYear) < point.validFromWeek)
				reasons += "Before start date"
			reasons
		}}
	}

}

trait ViewMeetingsForPointPermission extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewMeetingsForPointCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.View, point.pointSet.asInstanceOf[MonitoringPointSet])
	}
}

trait ViewMeetingsForPointCommandState {
	def student: StudentMember
	def point: MonitoringPoint
}

@Controller
@RequestMapping(Array("/{department}/{monitoringPoint}/meetings/{studentCourseDetails}"))
class ViewMeetingsForPointController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(@PathVariable studentCourseDetails: StudentMember,	@PathVariable monitoringPoint: MonitoringPoint) =
		ViewMeetingsForPointCommand(studentCourseDetails, monitoringPoint)

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[Seq[Pair[MeetingRecord, Seq[String]]]]) = {
		val meetingsStatuses = cmd.apply()
		Mav("home/meetings", "meetingsStatuses" -> meetingsStatuses).noLayoutIf(ajax)
	}
}
