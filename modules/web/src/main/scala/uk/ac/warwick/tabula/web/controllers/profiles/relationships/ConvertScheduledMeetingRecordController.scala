package uk.ac.warwick.tabula.web.controllers.profiles.relationships

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands.profiles.{ConvertScheduledMeetingRecordState, ConvertScheduledMeetingRecordCommand, CreateMeetingRecordCommand, ViewMeetingRecordCommand}
import uk.ac.warwick.tabula.web.controllers.profiles.{MeetingRecordAcademicYearFiltering, ProfilesController}
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.commands.{PopulateOnForm, Appliable, SelfValidating}
import uk.ac.warwick.tabula.profiles.web.Routes
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, AutowiringMonitoringPointMeetingRelationshipTermServiceComponent}
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringMeetingRecordServiceComponent

@Controller
@RequestMapping(value = Array("/profiles/{relationshipType}/meeting/{studentCourseDetails}/schedule/{meetingRecord}/confirm"))
class ConvertScheduledMeetingRecordController extends ProfilesController
	with AutowiringMonitoringPointMeetingRelationshipTermServiceComponent
	with AutowiringAttendanceMonitoringMeetingRecordServiceComponent
	with MeetingRecordAcademicYearFiltering
	with AutowiringTermServiceComponent {

	type PopulatableCommand = Appliable[MeetingRecord] with PopulateOnForm
	type ConvertScheduledMeetingRecordCommand = Appliable[MeetingRecord] with PopulateOnForm with ConvertScheduledMeetingRecordState

	validatesSelf[SelfValidating]

	@ModelAttribute("viewMeetingRecordCommand")
	def viewMeetingRecordCommand(
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable relationshipType: StudentRelationshipType
	) = {
		restricted(ViewMeetingRecordCommand(studentCourseDetails, optionalCurrentMember, relationshipType))
	}

	@ModelAttribute("convertCommand")
	def getConvertCommand(@PathVariable meetingRecord: ScheduledMeetingRecord) =  Option(meetingRecord).map(mr => {
		ConvertScheduledMeetingRecordCommand(currentMember, mr)
	}).orNull

	@ModelAttribute("command")
	def getCreateCommand(
		@PathVariable meetingRecord: ScheduledMeetingRecord,
		@PathVariable studentCourseDetails: StudentCourseDetails
	) = Option(meetingRecord).map(mr => {
		new CreateMeetingRecordCommand(currentMember, mr.relationship, considerAlternatives = false)
	}).orNull

	@RequestMapping(method=Array(GET, HEAD), params=Array("iframe"))
	def getIframe(
		@ModelAttribute("convertCommand") cmd: PopulatableCommand,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable meetingRecord: ScheduledMeetingRecord) =
			if(cmd != null) {
				cmd.populate()
				form(cmd, studentCourseDetails, meetingRecord, iframe = true)
			} else {
					Mav("profiles/related_students/meeting/was_deleted")
			}

	@RequestMapping(method=Array(GET, HEAD))
	def get(
		@ModelAttribute("convertCommand") cmd: PopulatableCommand,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable meetingRecord: ScheduledMeetingRecord) =
			if(cmd != null) {
				cmd.populate()
				form(cmd, studentCourseDetails, meetingRecord)
			} else {
				Mav("profiles/related_students/meeting/was_deleted")
			}

	private def form(
		cmd: Appliable[MeetingRecord],
		studentCourseDetails: StudentCourseDetails,
		meetingRecord: ScheduledMeetingRecord,
		iframe: Boolean = false
	) = {
		val mav = Mav("profiles/related_students/meeting/edit",
			"command" -> cmd,
			"isModal" -> ajax,
			"isIframe" -> iframe,
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
		@Valid @ModelAttribute("command") command: CreateMeetingRecordCommand,
		createErrors: Errors,
		@Valid @ModelAttribute("convertCommand") convertCommand: ConvertScheduledMeetingRecordCommand,
		convertErrors: Errors,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable meetingRecord: ScheduledMeetingRecord,
		@ModelAttribute("viewMeetingRecordCommand") viewCommand: Option[Appliable[Seq[AbstractMeetingRecord]]]) =
			if (command != null && convertCommand != null) {
					if (createErrors.hasErrors || convertErrors.hasErrors) {
						form(command, studentCourseDetails, meetingRecord, iframe = true)
					} else {
						convertCommand.createCommand = command
						val modifiedMeeting = convertCommand.apply()
						val meetingList = viewCommand match {
							case None => Seq()
							case Some(c) => c.apply().filterNot(meetingNotInAcademicYear(AcademicYear.guessSITSAcademicYearByDate(modifiedMeeting.meetingDate)))
						}
						Mav("profiles/related_students/meeting/list",
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
							"viewerUser" -> user,
							"viewerMember" -> currentMember,
							"openMeeting" -> modifiedMeeting).noLayout()
					}
				}
			else {
				Mav("profiles/related_students/meeting/was_deleted")
			}


	@RequestMapping(method=Array(POST))
	def submit(
		@Valid @ModelAttribute("command") command: CreateMeetingRecordCommand,
		createErrors: Errors,
		@Valid @ModelAttribute("convertCommand") convertCommand: ConvertScheduledMeetingRecordCommand,
		convertErrors: Errors,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable meetingRecord: ScheduledMeetingRecord) =
			if(command != null && convertCommand != null) {
				if (createErrors.hasErrors || convertErrors.hasErrors) {
					form(command, studentCourseDetails, meetingRecord, iframe = false)
				} else {
					convertCommand.createCommand = command
					convertCommand.apply()

					Redirect(Routes.oldProfile.view(studentCourseDetails.student))
				}
			} else {
					Mav("profiles/related_students/meeting/was_deleted")
			}
}
