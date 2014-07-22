package uk.ac.warwick.tabula.profiles.web.controllers.relationships

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.profiles.commands.{ConvertScheduledMeetingRecordState, ConvertScheduledMeetingRecordCommand, CreateMeetingRecordCommand, ViewMeetingRecordCommand}
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.commands.{PopulateOnForm, Appliable, SelfValidating}
import uk.ac.warwick.tabula.profiles.web.Routes
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.services.AutowiringMonitoringPointMeetingRelationshipTermServiceComponent
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringMeetingRecordServiceComponent

@Controller
@RequestMapping(value = Array("/{relationshipType}/meeting/{studentCourseDetails}/schedule/{meetingRecord}/confirm"))
class ConvertScheduledMeetingRecordController extends ProfilesController
	with AutowiringMonitoringPointMeetingRelationshipTermServiceComponent with AutowiringAttendanceMonitoringMeetingRecordServiceComponent {

	type PopulatableCommand = Appliable[MeetingRecord] with PopulateOnForm
	type ConvertScheduledMeetingRecordCommand = Appliable[MeetingRecord] with PopulateOnForm with ConvertScheduledMeetingRecordState

	validatesSelf[SelfValidating]

	@ModelAttribute("viewMeetingRecordCommand")
	def viewMeetingRecordCommand(
		@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails,
		@PathVariable("relationshipType") relationshipType: StudentRelationshipType
	) = {
		restricted(ViewMeetingRecordCommand(studentCourseDetails, optionalCurrentMember, relationshipType))
	}

	@ModelAttribute("convertCommand")
	def getConvertCommand(@PathVariable meetingRecord: ScheduledMeetingRecord) =  Option(meetingRecord).map(mr => {
		ConvertScheduledMeetingRecordCommand(currentMember, mr)
	})

	@ModelAttribute("command")
	def getCreateCommand(
		@PathVariable meetingRecord: ScheduledMeetingRecord,
		@PathVariable studentCourseDetails: StudentCourseDetails
	) = Option(meetingRecord).map(mr => {
		new CreateMeetingRecordCommand(currentMember, mr.relationship, considerAlternatives = false)
	})

	@RequestMapping(method=Array(GET, HEAD), params=Array("iframe"))
	def getIframe(
		@ModelAttribute("convertCommand") cmd: Option[PopulatableCommand],
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable meetingRecord: ScheduledMeetingRecord
	) = cmd match {
			case Some(command) =>
				command.populate()
				form(command, studentCourseDetails, meetingRecord, iframe = true)
			case None => Mav("related_students/meeting/was_deleted")
	}

	@RequestMapping(method=Array(GET, HEAD))
	def get(
		@ModelAttribute("convertCommand") cmd: Option[PopulatableCommand],
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable meetingRecord: ScheduledMeetingRecord
	) = cmd match {
		case Some(cmd: Appliable[MeetingRecord] with PopulateOnForm) =>
			cmd.populate()
			form(cmd, studentCourseDetails, meetingRecord)
		case None => Mav("related_students/meeting/was_deleted")
	}

	private def form(
		cmd: Appliable[MeetingRecord],
		studentCourseDetails: StudentCourseDetails,
		meetingRecord: ScheduledMeetingRecord,
		iframe: Boolean = false
	) = {
		val mav = Mav("related_students/meeting/edit",
			"command" -> cmd,
			"modal" -> ajax,
			"iframe" -> iframe,
			"studentCourseDetails" -> studentCourseDetails,
			"isStudent" -> (studentCourseDetails.student == currentMember),
			"relationshipType"-> meetingRecord.relationship.relationshipType,
			"creator" -> meetingRecord.creator,
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
		@Valid @ModelAttribute("command") command: Option[CreateMeetingRecordCommand],
		createErrors: Errors,
		@Valid @ModelAttribute("convertCommand") convertCommand: Option[ConvertScheduledMeetingRecordCommand],
		convertErrors: Errors,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable meetingRecord: ScheduledMeetingRecord,
		@ModelAttribute("viewMeetingRecordCommand") viewCommand: Option[Appliable[Seq[AbstractMeetingRecord]]]
	) = (command, convertCommand) match {
			case (Some(cmd), Some(convertCmd)) =>
				if (createErrors.hasErrors || convertErrors.hasErrors) {
					form(cmd, studentCourseDetails, meetingRecord, iframe = true)
				} else {
						convertCmd.createCommand = cmd
						val modifiedMeeting = convertCmd.apply()

						val meetingList = viewCommand match {
							case None => Seq()
							case Some(c) => c.apply()
						}
						Mav("related_students/meeting/list",
							"studentCourseDetails" -> studentCourseDetails,
							"role" -> modifiedMeeting.relationship.relationshipType,
							"meetings" -> meetingList,
							"meetingApprovalWillCreateCheckpoint" -> meetingList.map {
								case (meeting: MeetingRecord) => meeting.id -> (
									monitoringPointMeetingRelationshipTermService.willCheckpointBeCreated(meeting)
										|| attendanceMonitoringMeetingRecordService.getCheckpoints(meeting).nonEmpty
									)
								case (meeting: ScheduledMeetingRecord) => meeting.id -> false
							}.toMap,
							"viewer" -> currentMember,
							"openMeeting" -> modifiedMeeting).noLayout()
				}
			case _ => Mav("related_students/meeting/was_deleted")
	}


	@RequestMapping(method=Array(POST))
	def submit(
		@Valid @ModelAttribute("command") command: Option[CreateMeetingRecordCommand],
		createErrors: Errors,
		@Valid @ModelAttribute("convertCommand") convertCommand: Option[ConvertScheduledMeetingRecordCommand],
		convertErrors: Errors,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable meetingRecord: ScheduledMeetingRecord
	) = (command, convertCommand) match {
		case (Some(cmd), Some(convertCmd)) =>
			if (createErrors.hasErrors || convertErrors.hasErrors) {
				form(cmd, studentCourseDetails, meetingRecord, iframe = false)
			} else {
				convertCmd.createCommand = cmd
				convertCmd.apply()

				Redirect(Routes.profile.view(studentCourseDetails.student))
			}
		case _ => Mav("related_students/meeting/was_deleted")
	}
}

