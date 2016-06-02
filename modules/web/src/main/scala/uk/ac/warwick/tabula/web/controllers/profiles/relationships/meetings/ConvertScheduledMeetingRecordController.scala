package uk.ac.warwick.tabula.web.controllers.profiles.relationships.meetings

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.profiles.relationships.meetings._
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, _}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringMeetingRecordServiceComponent
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointMeetingRelationshipTermServiceComponent, AutowiringTermServiceComponent}
import uk.ac.warwick.tabula.web.controllers.profiles.{MeetingRecordAcademicYearFiltering, ProfilesController}

@Controller
@RequestMapping(value = Array("/profiles/{relationshipType}/meeting/{studentCourseDetails}/{academicYear}/schedule/{meetingRecord}/confirm"))
class ConvertScheduledMeetingRecordController extends ProfilesController
	with AutowiringMonitoringPointMeetingRelationshipTermServiceComponent
	with AutowiringAttendanceMonitoringMeetingRecordServiceComponent
	with MeetingRecordAcademicYearFiltering
	with AutowiringTermServiceComponent {

	type PopulatableCommand = Appliable[MeetingRecord] with PopulateOnForm
	type ConvertScheduledMeetingRecordCommand = Appliable[MeetingRecord] with PopulateOnForm with ConvertScheduledMeetingRecordState

	validatesSelf[SelfValidating]

	@ModelAttribute("convertCommand")
	def getConvertCommand(@PathVariable meetingRecord: ScheduledMeetingRecord) = {
		Option(meetingRecord).map(mr => {
			ConvertScheduledMeetingRecordCommand(currentMember, mr)
		}).orNull
	}

	@ModelAttribute("command")
	def getCreateCommand(@PathVariable meetingRecord: ScheduledMeetingRecord) = {
		Option(meetingRecord).map(mr => {
			CreateMeetingRecordCommand(currentMember, mr.relationship)
		}).orNull
	}

	@RequestMapping(method=Array(GET, HEAD), params=Array("iframe"))
	def getIframe(
		@ModelAttribute("convertCommand") cmd: PopulatableCommand,
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable academicYear: AcademicYear
	) = {
		if (cmd != null) {
			cmd.populate()
			form(cmd, relationshipType, studentCourseDetails, academicYear, iframe = true)
		} else {
			Mav("profiles/related_students/meeting/was_deleted")
		}
	}

	@RequestMapping(method=Array(GET, HEAD))
	def get(
		@ModelAttribute("convertCommand") cmd: PopulatableCommand,
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable academicYear: AcademicYear
	) = {
		if (cmd != null) {
			cmd.populate()
			form(cmd, relationshipType, studentCourseDetails, academicYear)
		} else {
			Mav("profiles/related_students/meeting/was_deleted")
		}
	}

	private def form(
		cmd: Appliable[MeetingRecord],
		relationshipType: StudentRelationshipType,
		studentCourseDetails: StudentCourseDetails,
		academicYear: AcademicYear,
		iframe: Boolean = false
	) = {
		val mav = Mav("profiles/related_students/meeting/edit",
			"command" -> cmd,
			"returnTo" -> getReturnTo(Routes.Profile.relationshipType(studentCourseDetails, academicYear, relationshipType)),
			"isModal" -> ajax,
			"isIframe" -> iframe,
			"formats" -> MeetingFormat.members,
			"isStudent" -> (studentCourseDetails.student == currentMember)
		)
		if (ajax)
			mav.noLayout()
		else if (iframe)
			mav.noNavigation()
		else
			mav
	}

	@RequestMapping(method=Array(POST), params=Array("iframe"))
	def submitIframe(
		@Valid @ModelAttribute("command") command: Appliable[MeetingRecord],
		createErrors: Errors,
		@Valid @ModelAttribute("convertCommand") convertCommand: ConvertScheduledMeetingRecordCommand,
		convertErrors: Errors,
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable academicYear: AcademicYear
	) = {
		if (command != null && convertCommand != null) {
			if (createErrors.hasErrors || convertErrors.hasErrors) {
				form(command, relationshipType, studentCourseDetails, academicYear, iframe = true)
			} else {
				convertCommand.createCommand = command
				convertCommand.apply()
				Mav("profiles/related_students/meeting/edit",
					"success" -> true
				)
			}
		} else {
			Mav("profiles/related_students/meeting/was_deleted")
		}
	}


	@RequestMapping(method=Array(POST))
	def submit(
		@Valid @ModelAttribute("command") command: Appliable[MeetingRecord],
		createErrors: Errors,
		@Valid @ModelAttribute("convertCommand") convertCommand: ConvertScheduledMeetingRecordCommand,
		convertErrors: Errors,
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable academicYear: AcademicYear
	) = {
		if(command != null && convertCommand != null) {
			if (createErrors.hasErrors || convertErrors.hasErrors) {
				form(command, relationshipType, studentCourseDetails, academicYear, iframe = false)
			} else {
				convertCommand.createCommand = command
				convertCommand.apply()

				Redirect(Routes.Profile.relationshipType(studentCourseDetails, academicYear, relationshipType))
			}
		} else {
				Mav("profiles/related_students/meeting/was_deleted")
		}
	}
}
