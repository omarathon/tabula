package uk.ac.warwick.tabula.profiles.web.controllers.relationships

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.profiles.commands.{ViewMeetingRecordCommand, CreateScheduledMeetingRecordCommand}
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.profiles.web.Routes
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.services.AutowiringMonitoringPointMeetingRelationshipTermServiceComponent

@Controller
@RequestMapping(value = Array("/{relationshipType}/meeting/{studentCourseDetails}/schedule/create"))
class CreateScheduledMeetingRecordController extends ProfilesController with AutowiringMonitoringPointMeetingRelationshipTermServiceComponent {

	validatesSelf[SelfValidating]

	@ModelAttribute("allRelationships")
	def allRelationships(@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails,
											 @PathVariable("relationshipType") relationshipType: StudentRelationshipType) = {

		relationshipService.findCurrentRelationships(relationshipType, studentCourseDetails.sprCode)
	}

	@ModelAttribute("viewMeetingRecordCommand")
	def viewMeetingRecordCommand(
		@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails,
		@PathVariable("relationshipType") relationshipType: StudentRelationshipType
	) = {
		restricted(ViewMeetingRecordCommand(studentCourseDetails, optionalCurrentMember, relationshipType))
	}

	@ModelAttribute("command")
	def getCommand(@PathVariable("relationshipType") relationshipType: StudentRelationshipType,
				   @PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails) =  {
		relationshipService.findCurrentRelationships(mandatory(relationshipType), mandatory(studentCourseDetails).sprCode) match {
			case Nil => throw new ItemNotFoundException
			case relationships =>
				// Go through the relationships for this SPR code and find one where the current user is the agent.
				// If there isn't one but there's only one relationship, pick it. Otherwise default to the first.
				val defaultRelationship =
					relationships.find(rel => rel.agentMember.map(_.universityId) == Some(user.universityId))
						.getOrElse(relationships.head)

				CreateScheduledMeetingRecordCommand(currentMember, defaultRelationship, relationships.size > 1)
		}
	}

	@RequestMapping(method=Array(GET, HEAD), params=Array("iframe"))
	def getIframe(
	 @ModelAttribute("command") cmd: Appliable[ScheduledMeetingRecord],
		@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails
 	) = {
		form(cmd, studentCourseDetails, iframe = true)
	}

	@RequestMapping(method=Array(GET, HEAD))
	def get(
	 @ModelAttribute("command") cmd: Appliable[ScheduledMeetingRecord],
	 @PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails
 	) = {
		form(cmd, studentCourseDetails)
	}

	private def form(
		cmd: Appliable[ScheduledMeetingRecord],
		studentCourseDetails: StudentCourseDetails,
		iframe: Boolean = false
	) = {
		val mav = Mav("related_students/meeting/schedule",
			"returnTo" -> getReturnTo(Routes.profile.view(studentCourseDetails.student)),
			"isModal" -> ajax,
			"iframe" -> iframe,
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
		@Valid @ModelAttribute("command") cmd: Appliable[ScheduledMeetingRecord],
		errors: Errors,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable("relationshipType") relationshipType: StudentRelationshipType,
		@ModelAttribute("viewMeetingRecordCommand") viewCommand: Option[Appliable[Seq[AbstractMeetingRecord]]]
	) = {
		if (errors.hasErrors) {
			form(cmd, studentCourseDetails, iframe = true)
		} else {
			val modifiedMeeting = cmd.apply()
			val meetingList = viewCommand match {
				case None => Seq()
				case Some(command) => command.apply()
			}
			Mav("related_students/meeting/list",
				"studentCourseDetails" -> studentCourseDetails,
				"role" -> relationshipType,
				"meetings" -> meetingList,
				"meetingApprovalWillCreateCheckpoint" -> meetingList.map {
					case (meeting: MeetingRecord) => meeting.id -> monitoringPointMeetingRelationshipTermService.willCheckpointBeCreated(meeting)
					case (meeting: ScheduledMeetingRecord) => meeting.id -> false
				}.toMap,
				"viewer" -> currentMember,
				"openMeeting" -> modifiedMeeting).noLayout()
		}
	}

	@RequestMapping(method=Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[ScheduledMeetingRecord],
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

