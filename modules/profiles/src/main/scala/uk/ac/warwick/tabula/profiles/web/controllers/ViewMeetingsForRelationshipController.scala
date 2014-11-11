package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.profiles.commands.ViewMeetingRecordCommand
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringMeetingRecordServiceComponent
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointMeetingRelationshipTermServiceComponent, AutowiringTermServiceComponent}
import uk.ac.warwick.util.termdates.{Term, TermNotFoundException}

@Controller
@RequestMapping(Array("/view/meetings/{studentRelationshipType}/{studentCourseDetails}/{academicYear}"))
class ViewMeetingsForRelationshipController
	extends ProfilesController with AutowiringTermServiceComponent with AutowiringMonitoringPointMeetingRelationshipTermServiceComponent
	with AutowiringAttendanceMonitoringMeetingRecordServiceComponent with MeetingRecordAcademicYearFiltering {

	@RequestMapping
	def home(
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable studentRelationshipType: StudentRelationshipType,
		@PathVariable academicYear: AcademicYear,
		@RequestParam(value = "meeting", required = false) openMeetingId: String) = {
		val meetings = ViewMeetingRecordCommand(
			mandatory(studentCourseDetails),
			optionalCurrentMember,
			mandatory(studentRelationshipType)
		).apply().filterNot(meetingNotInAcademicYear(mandatory(academicYear)))

		Mav("related_students/meeting/list",
			"meetings" -> meetings,
			"role" -> mandatory(studentRelationshipType),
			"viewerUser" -> user,
			"viewerMember" -> currentMember,
			"openMeetingId" -> openMeetingId,
			"meetingApprovalWillCreateCheckpoint" -> meetings.map {
				case (meeting: MeetingRecord) => meeting.id -> (
					monitoringPointMeetingRelationshipTermService.willCheckpointBeCreated(meeting)
						|| attendanceMonitoringMeetingRecordService.getCheckpoints(meeting).nonEmpty
					)
				case (meeting: ScheduledMeetingRecord) => meeting.id -> false
			}.toMap
		).noLayoutIf(ajax)
	}

}
