package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{ScheduledMeetingRecord, MeetingRecord, StudentCourseDetails, StudentRelationshipType}
import uk.ac.warwick.tabula.profiles.commands.ViewMeetingRecordCommand
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringMeetingRecordServiceComponent
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointMeetingRelationshipTermServiceComponent, AutowiringTermServiceComponent}
import uk.ac.warwick.util.termdates.{Term, TermNotFoundException}

@Controller
@RequestMapping(Array("/view/meetings/{studentRelationshipType}/{studentCourseDetails}/{academicYear}"))
class ViewMeetingsForRelationshipController
	extends ProfilesController with AutowiringTermServiceComponent with AutowiringMonitoringPointMeetingRelationshipTermServiceComponent
	with AutowiringAttendanceMonitoringMeetingRecordServiceComponent {

	@RequestMapping
	def home(
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable studentRelationshipType: StudentRelationshipType,
		@PathVariable academicYear: AcademicYear
	) = {
		val meetings = ViewMeetingRecordCommand(
			mandatory(studentCourseDetails),
			optionalCurrentMember,
			mandatory(studentRelationshipType)
		).apply().filterNot { meeting =>
			try {
				Seq(Term.WEEK_NUMBER_BEFORE_START, Term.WEEK_NUMBER_AFTER_END).contains(
					termService.getAcademicWeekForAcademicYear(meeting.meetingDate, mandatory(academicYear))
				)
			} catch {
				case e: TermNotFoundException =>
					// TAB-2465 Don't include this meeting - this happens if you are looking at a year before we recorded term dates
					true
			}
		}
		Mav("related_students/meeting/list",
			"meetings" -> meetings,
			"role" -> mandatory(studentRelationshipType),
			"viewer" -> currentMember,
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
