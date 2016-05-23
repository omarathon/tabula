package uk.ac.warwick.tabula.web.controllers.profiles.relationships

import javax.validation.Valid

import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.profiles.{ModifyMeetingRecordCommand, ViewMeetingRecordCommand}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, _}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringMeetingRecordServiceComponent
import uk.ac.warwick.tabula.services.{MonitoringPointMeetingRelationshipTermServiceComponent, ProfileServiceComponent, RelationshipServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.web.controllers.profiles.{CurrentMemberComponent, MeetingRecordAcademicYearFiltering}
import uk.ac.warwick.tabula.web.controllers.{ControllerImports, ControllerMethods, ControllerViews}


trait MeetingRecordModal extends MeetingRecordAcademicYearFiltering {
	self: ProfileServiceComponent
		with RelationshipServiceComponent
		with ControllerMethods
		with ControllerImports
		with CurrentMemberComponent
		with ControllerViews
		with MonitoringPointMeetingRelationshipTermServiceComponent
		with AttendanceMonitoringMeetingRecordServiceComponent
		with TermServiceComponent =>
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
	 *	def getCommand(@PathVariable meetingRecord: MeetingRecord) = new EditMeetingRecordCommand(meetingRecord)
	 *
	 */


	@ModelAttribute("allRelationships")
	def allRelationships(
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable relationshipType: StudentRelationshipType
	) = {
		relationshipService.findCurrentRelationships(relationshipType, studentCourseDetails)
	}

	@ModelAttribute("viewMeetingRecordCommand")
	def viewMeetingRecordCommand(
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable relationshipType: StudentRelationshipType
	) = {
		restricted(ViewMeetingRecordCommand(studentCourseDetails, optionalCurrentMember, relationshipType))
	}

	// modal chrome
	@RequestMapping(method = Array(GET, HEAD), params = Array("modal"))
	def showModalChrome(
		@ModelAttribute("command") command: ModifyMeetingRecordCommand,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable relationshipType: StudentRelationshipType
	) = {

		Mav("profiles/related_students/meeting/edit",
			"isModal" -> true,
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
		@PathVariable studentCourseDetails:StudentCourseDetails,
		@PathVariable relationshipType: StudentRelationshipType
	) = {
		val formats = MeetingFormat.members

		Mav("profiles/related_students/meeting/edit",
			"isIframe" -> true,
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
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable relationshipType: StudentRelationshipType
	) = transactional() {
		if (errors.hasErrors) {
			showIframeForm(command, studentCourseDetails, relationshipType)
		} else {
			val modifiedMeeting = command.apply()
			val meetingList = viewCommand match {
				case None => Seq()
				case Some(cmd) => cmd.apply().filterNot(meetingNotInAcademicYear(AcademicYear.guessSITSAcademicYearByDate(modifiedMeeting.meetingDate)))
			}

			Mav("profiles/related_students/meeting/list",
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
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable relationshipType: StudentRelationshipType
	) = {
		val formats = MeetingFormat.members

		Mav("profiles/related_students/meeting/edit",
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
	def cancel(@PathVariable studentCourseDetails: StudentCourseDetails) = {
		Redirect(Routes.oldProfile.view(studentCourseDetails.student))
	}

	// submit sync
	@RequestMapping(method = Array(POST), params = Array("submit"))
	def saveMeetingRecord(
		@Valid @ModelAttribute("command") command: ModifyMeetingRecordCommand,
		errors: Errors,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable relationshipType: StudentRelationshipType
	) = transactional() {
		if (errors.hasErrors) {
			showForm(command, studentCourseDetails, relationshipType)
		} else {
			val meeting = command.apply()
			Redirect(Routes.oldProfile.view(studentCourseDetails.student, meeting))
		}
	}

}
