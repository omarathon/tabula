package uk.ac.warwick.tabula.web.controllers.profiles.relationships

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.profiles.{EditScheduledMeetingRecordCommand, ScheduledMeetingRecordResult, ViewMeetingRecordCommand}
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, _}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringMeetingRecordServiceComponent
import uk.ac.warwick.tabula.web.controllers.profiles.{MeetingRecordAcademicYearFiltering, ProfilesController}

@Controller
@RequestMapping(value = Array("/profiles/{relationshipType}/meeting/{studentCourseDetails}/schedule/{meetingRecord}/edit"))
class EditScheduledMeetingRecordController extends ProfilesController
	with AutowiringAttendanceMonitoringMeetingRecordServiceComponent
	with MeetingRecordAcademicYearFiltering
	with AutowiringTermServiceComponent {

	validatesSelf[SelfValidating]

	@ModelAttribute("viewMeetingRecordCommand")
	def viewMeetingRecordCommand(
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable relationshipType: StudentRelationshipType
	) = {
		restricted(ViewMeetingRecordCommand(studentCourseDetails, optionalCurrentMember, relationshipType))
	}

	@ModelAttribute("command")
	def getCommand(@PathVariable meetingRecord: ScheduledMeetingRecord) =  {
		EditScheduledMeetingRecordCommand(currentMember, meetingRecord)
	}

	@RequestMapping(method=Array(GET, HEAD), params=Array("iframe"))
	def getIframe(
	 @ModelAttribute("command") cmd: Appliable[ScheduledMeetingRecordResult] with PopulateOnForm,
		@PathVariable studentCourseDetails: StudentCourseDetails
	) = {
		cmd.populate()
		form(cmd, studentCourseDetails, iframe = true)
	}

	@RequestMapping(method=Array(GET, HEAD))
	def get(
	 @ModelAttribute("command") cmd: Appliable[ScheduledMeetingRecordResult] with PopulateOnForm,
	 @PathVariable studentCourseDetails: StudentCourseDetails
	) = {
		cmd.populate()
		form(cmd, studentCourseDetails)
	}

	private def form(
		cmd: Appliable[ScheduledMeetingRecordResult] with PopulateOnForm,
		studentCourseDetails: StudentCourseDetails,
		iframe: Boolean = false
	) = {
		val mav = Mav("profiles/related_students/meeting/schedule",
			"returnTo" -> getReturnTo(Routes.profile.view(studentCourseDetails.student)),
			"isModal" -> ajax,
			"iframe" -> iframe,
			"edit" -> true,
			"formats" -> MeetingFormat.members
		)
		if(ajax)
			mav.noLayout()
		else if (iframe)
			mav.noNavigation()
		else
			mav
	}

	@RequestMapping(method=Array(POST), params=Array("iframe"))
	def submitIframe(
		@Valid @ModelAttribute("command") cmd: Appliable[ScheduledMeetingRecordResult] with PopulateOnForm,
		errors: Errors,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable relationshipType: StudentRelationshipType,
		@ModelAttribute("viewMeetingRecordCommand") viewCommand: Option[Appliable[Seq[AbstractMeetingRecord]]]
	) = {
		if (errors.hasErrors) {
			form(cmd, studentCourseDetails, iframe = true)
		} else {
			val modifiedMeeting = cmd.apply().meetingRecord
			val meetingList = viewCommand match {
				case None => Seq()
				case Some(command) => command.apply().filterNot(meetingNotInAcademicYear(AcademicYear.guessSITSAcademicYearByDate(modifiedMeeting.meetingDate)))
			}
			Mav("profiles/related_students/meeting/list",
				"studentCourseDetails" -> studentCourseDetails,
				"role" -> relationshipType,
				"meetings" -> meetingList,
				"meetingApprovalWillCreateCheckpoint" -> meetingList.map {
					case (meeting: MeetingRecord) => meeting.id -> attendanceMonitoringMeetingRecordService.getCheckpoints(meeting).nonEmpty
					case (meeting: ScheduledMeetingRecord) => meeting.id -> false
				}.toMap,
				"viewerUser" -> user,
				"viewerMember" -> currentMember,
				"openMeeting" -> modifiedMeeting).noLayout()
		}
	}

	@RequestMapping(method=Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[ScheduledMeetingRecordResult] with PopulateOnForm,
		errors: Errors,
		@PathVariable studentCourseDetails: StudentCourseDetails
	) = {
		if (errors.hasErrors) {
			form(cmd, studentCourseDetails)
		} else {
			cmd.apply()
			Redirect(Routes.profile.view(studentCourseDetails.student))
		}
	}

}
