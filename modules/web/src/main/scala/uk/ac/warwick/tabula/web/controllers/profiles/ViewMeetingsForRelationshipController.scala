package uk.ac.warwick.tabula.web.controllers.profiles

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.profiles.ViewMeetingRecordCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringMeetingRecordServiceComponent

@Controller
@RequestMapping(Array("/profiles/view/meetings/{studentRelationshipType}/{studentCourseDetails}/{academicYear}"))
class ViewMeetingsForRelationshipController
	extends ProfilesController with AutowiringTermServiceComponent with AutowiringAttendanceMonitoringMeetingRecordServiceComponent
		with MeetingRecordAcademicYearFiltering {

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

		Mav("profiles/related_students/meeting/list",
			"meetings" -> meetings,
			"role" -> mandatory(studentRelationshipType),
			"viewerUser" -> user,
			"viewerMember" -> currentMember,
			"openMeetingId" -> openMeetingId,
			"meetingApprovalWillCreateCheckpoint" -> meetings.map {
				case (meeting: MeetingRecord) => meeting.id -> attendanceMonitoringMeetingRecordService.getCheckpoints(meeting).nonEmpty
				case (meeting: ScheduledMeetingRecord) => meeting.id -> false
			}.toMap
		).noLayoutIf(ajax)
	}

}
