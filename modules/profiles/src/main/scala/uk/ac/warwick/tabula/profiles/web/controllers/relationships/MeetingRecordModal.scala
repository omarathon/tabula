package uk.ac.warwick.tabula.profiles.web.controllers.relationships

import javax.validation.Valid

import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, _}
import uk.ac.warwick.tabula.profiles.commands.{ModifyMeetingRecordCommand, ViewMeetingRecordCommand}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.profiles.web.controllers.CurrentMemberComponent
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringMeetingRecordServiceComponent
import uk.ac.warwick.tabula.services.{MonitoringPointMeetingRelationshipTermServiceComponent, ProfileServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.web.controllers.{ControllerImports, ControllerMethods, ControllerViews}


trait MeetingRecordModal {

	this: ProfileServiceComponent with RelationshipServiceComponent with ControllerMethods with ControllerImports with CurrentMemberComponent with ControllerViews
		with MonitoringPointMeetingRelationshipTermServiceComponent with AttendanceMonitoringMeetingRecordServiceComponent =>
	/**
	 * Contains all of the request mappings needed to drive meeting record modals (including iframe stuff)
	 *
	 * Implementers must have a validatesSelf clause and a ModelAttribute definition that returns an implementation of
	 * ModifyAssignmentCommand
	 *
	 * e.g.
	 *
	 * validatesSelf[CreateMeetingRecordCommand]
	 * 	\@ModelAttribute("command")
	 *	def getCommand(@PathVariable("meeting") meetingRecord: MeetingRecord) = new EditMeetingRecordCommand(meetingRecord)
	 *
	 */


	@ModelAttribute("allRelationships")
	def allRelationships(
		@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails,
		@PathVariable("relationshipType") relationshipType: StudentRelationshipType
	) = {
		relationshipService.findCurrentRelationships(relationshipType, studentCourseDetails)
	}

	@ModelAttribute("viewMeetingRecordCommand")
	def viewMeetingRecordCommand(
		@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails,
		@PathVariable("relationshipType") relationshipType: StudentRelationshipType
	) = {
		restricted(ViewMeetingRecordCommand(studentCourseDetails, optionalCurrentMember, relationshipType))
	}

	// modal chrome
	@RequestMapping(method = Array(GET, HEAD), params = Array("modal"))
	def showModalChrome(
		@ModelAttribute("command") command: ModifyMeetingRecordCommand,
		@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails,
		@PathVariable("relationshipType") relationshipType: StudentRelationshipType
	) = {

		Mav("related_students/meeting/edit",
			"modal" -> true,
			"agentName" -> (if (command.considerAlternatives) "" else command.relationship.agentName),
			"studentCourseDetails" -> studentCourseDetails,
			"isStudent" -> (studentCourseDetails.student == currentMember),
			"relationshipType" -> relationshipType
		).noLayout()
	}

	// modal iframe form
	@RequestMapping(method = Array(GET, HEAD), params = Array("iframe"))
	def showIframeForm(
		@ModelAttribute("command") command: ModifyMeetingRecordCommand,
		@PathVariable("studentCourseDetails") studentCourseDetails:StudentCourseDetails,
		@PathVariable("relationshipType") relationshipType: StudentRelationshipType
	) = {
		val formats = MeetingFormat.members

		Mav("related_students/meeting/edit",
			"iframe" -> true,
			"command" -> command,
			"studentCourseDetails" -> studentCourseDetails,
			"isStudent" -> (studentCourseDetails.student == currentMember),
			"relationshipType"->relationshipType,
			"creator" -> command.creator,
			"formats" -> formats
		).noNavigation()
	}

	// submit async
	@RequestMapping(method = Array(POST), params = Array("modal"))
	def saveModalMeetingRecord(
		@Valid @ModelAttribute("command") command: ModifyMeetingRecordCommand,
		errors: Errors,
		@ModelAttribute("viewMeetingRecordCommand") viewCommand: Option[Appliable[Seq[AbstractMeetingRecord]]],
		@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails,
		@PathVariable("relationshipType") relationshipType: StudentRelationshipType
	) = transactional() {
		if (errors.hasErrors) {
			showIframeForm(command, studentCourseDetails, relationshipType)
		} else {
			val modifiedMeeting = command.apply()
			val meetingList = viewCommand match {
				case None => Seq()
				case Some(cmd) => cmd.apply()
			}

			Mav("related_students/meeting/list",
        	"studentCourseDetails" -> studentCourseDetails,
 			    "role" -> relationshipType,
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

	// blank sync form
	@RequestMapping(method = Array(GET, HEAD))
	def showForm(
		@ModelAttribute("command") command: ModifyMeetingRecordCommand,
		@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails,
		@PathVariable("relationshipType") relationshipType: StudentRelationshipType
	) = {
		val formats = MeetingFormat.members

		Mav("related_students/meeting/edit",
			"command" -> command,
			"studentCourseDetails" -> studentCourseDetails,
			"isStudent" -> (studentCourseDetails.student == currentMember),
		  "relationshipType"->relationshipType,
			"agentName" -> command.relationship.agentName,
			"creator" -> command.creator,
			"formats" -> formats
		)
	}

	// cancel sync
	@RequestMapping(method = Array(POST), params = Array("!submit", "!modal"))
	def cancel(@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails) = {
		Redirect(Routes.profile.view(studentCourseDetails.student))
	}

	// submit sync
	@RequestMapping(method = Array(POST), params = Array("submit"))
	def saveMeetingRecord(
		@Valid @ModelAttribute("command") command: ModifyMeetingRecordCommand,
		errors: Errors,
		@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails,
		@PathVariable("relationshipType") relationshipType: StudentRelationshipType
	) = transactional() {
		if (errors.hasErrors) {
			showForm(command, studentCourseDetails, relationshipType)
		} else {
			val meeting = command.apply()
			Redirect(Routes.profile.view(studentCourseDetails.student, meeting))
		}
	}

}
