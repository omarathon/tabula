package uk.ac.warwick.tabula.profiles.web.controllers.tutor

import org.springframework.web.bind.annotation.{InitBinder, PathVariable, ModelAttribute}
import uk.ac.warwick.tabula.profiles.commands.{ViewMeetingRecordCommand, ModifyMeetingRecordCommand, CreateMeetingRecordCommand}
import uk.ac.warwick.tabula.data.model.{StudentRelationship, StudentMember, MeetingFormat, Member}
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.Transactions._
import scala.Some
import uk.ac.warwick.tabula.profiles.web.Routes
import org.springframework.web.bind.WebDataBinder
import uk.ac.warwick.util.web.bind.AbstractPropertyEditor
import uk.ac.warwick.tabula.data.model.RelationshipType.PersonalTutor
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.StudentCourseDetails

trait MeetingRecordModal extends ProfilesController {
	var relationshipService = Wire.auto[RelationshipService]

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
	def allRelationships(@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails) =
		relationshipService.findCurrentRelationships(PersonalTutor, studentCourseDetails.sprCode)

	@ModelAttribute("viewMeetingRecordCommand")
	def viewMeetingRecordCommand(@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails) =
		restricted(new ViewMeetingRecordCommand(studentCourseDetails, user))

	// modal chrome
	@RequestMapping(method = Array(GET, HEAD), params = Array("modal"))
	def showModalChrome(@ModelAttribute("command") command: ModifyMeetingRecordCommand, @PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails) = {
		val formats = MeetingFormat.members

		Mav("tutor/meeting/edit",
			"modal" -> true,
			"command" -> command,
			"studentCourseDetails" -> studentCourseDetails,
			"isStudent" -> (studentCourseDetails.student == currentMember),
			"tutorName" -> command.relationship.agentName).noLayout()
	}

	// modal iframe form
	@RequestMapping(method = Array(GET, HEAD), params = Array("iframe"))
	def showIframeForm(@ModelAttribute("command") command: ModifyMeetingRecordCommand, @PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails) = {

		val formats = MeetingFormat.members

		Mav("tutor/meeting/edit",
			"iframe" -> true,
			"command" -> command,
			"studentCourseDetails" -> studentCourseDetails,
			"isStudent" -> (studentCourseDetails.student == currentMember),
			"tutorName" -> command.relationship.agentName,
			"creator" -> command.creator,
			"formats" -> formats).noNavigation()
	}

	// submit async
	@RequestMapping(method = Array(POST), params = Array("modal"))
	def saveModalMeetingRecord(@Valid @ModelAttribute("command")command: ModifyMeetingRecordCommand,
	                           errors: Errors,
	                           @ModelAttribute("viewMeetingRecordCommand") viewCommand: Option[ViewMeetingRecordCommand],
	                           @PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails) =transactional() {
		if (errors.hasErrors) {
			showIframeForm(command, studentCourseDetails)
		} else {
			val modifiedMeeting = command.apply()
			val meetingList = viewCommand match {
				case None => Seq()
				case Some(cmd) => cmd.apply
			}

			Mav("tutor/meeting/list",
				"studentCourseDetails" -> studentCourseDetails,
				"meetings" -> meetingList,
				"viewer" -> currentMember,
				"openMeeting" -> modifiedMeeting).noLayout()
		}
	}

	// blank sync form
	@RequestMapping(method = Array(GET, HEAD))
	def showForm(@ModelAttribute("command") command: ModifyMeetingRecordCommand, @PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails) = {
		val formats = MeetingFormat.members

		Mav("tutor/meeting/edit",
			"command" -> command,
			"studentCourseDetails" -> studentCourseDetails,
			"isStudent" -> (studentCourseDetails.student == currentMember),
			"tutorName" -> command.relationship.agentName,
			"creator" -> command.creator,
			"formats" -> formats)
	}

	// cancel sync
	@RequestMapping(method = Array(POST), params = Array("!submit", "!modal"))
	def cancel(@PathVariable("student") student: Member) = {
		Redirect(Routes.profile.view(student))
	}

	// submit sync
	@RequestMapping(method = Array(POST), params = Array("submit"))
	def saveMeetingRecord(@Valid @ModelAttribute("createMeetingRecordCommand") createCommand: CreateMeetingRecordCommand, errors: Errors, @PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails) = {
		transactional() {
			if (errors.hasErrors) {
				showForm(createCommand, studentCourseDetails)
			} else {
				val meeting = createCommand.apply()
				Redirect(Routes.profile.view(studentCourseDetails.student, meeting))
			}
		}
	}

	@InitBinder
	def initRelationshipsEditor(binder: WebDataBinder, @PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails) {
		binder.registerCustomEditor(classOf[StudentRelationship], new AbstractPropertyEditor[StudentRelationship] {
			override def fromString(agent: String) = allRelationships(studentCourseDetails).find(_.agent == agent).orNull
			override def toString(rel: StudentRelationship) = rel.agent
		})
	}

}
