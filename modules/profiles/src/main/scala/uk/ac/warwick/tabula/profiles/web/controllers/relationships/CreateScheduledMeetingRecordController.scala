package uk.ac.warwick.tabula.profiles.web.controllers.relationships

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.profiles.commands.CreateScheduledMeetingRecordCommand
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.profiles.web.Routes
import javax.validation.Valid
import org.springframework.validation.Errors

@Controller
@RequestMapping(value = Array("/{relationshipType}/meeting/{studentCourseDetails}/schedule/create"))
class CreateScheduledMeetingRecordController extends ProfilesController {

	validatesSelf[SelfValidating]

	@ModelAttribute("allRelationships")
	def allRelationships(@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails,
											 @PathVariable("relationshipType") relationshipType: StudentRelationshipType) = {

		relationshipService.findCurrentRelationships(relationshipType, studentCourseDetails.sprCode)
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

	@RequestMapping(method=Array(GET, HEAD), params=Array("isIframe"))
	def getIframe(
	 @ModelAttribute("command") cmd: Appliable[ScheduledMeetingRecord],
		@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails
 	) = {
		form(cmd, studentCourseDetails, isIframe = true)
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
		isIframe: Boolean = false
	) = {
		val mav = Mav("related_students/meeting/schedule",
			"returnTo" -> getReturnTo(Routes.profile.view(studentCourseDetails.student)),
			"isModal" -> ajax,
			"isIframe" -> isIframe,
			"formats" -> MeetingFormat.members
		)
		if(ajax)
			mav.noLayout()
		else if (isIframe)
			mav.noNavigation()
		else
			mav
	}

	@RequestMapping(method=Array(POST), params=Array("isIframe"))
	def submitIframe(
		@Valid @ModelAttribute("command") cmd: Appliable[ScheduledMeetingRecord],
		errors: Errors,
		@PathVariable studentCourseDetails: StudentCourseDetails
	) = {
		if (errors.hasErrors) {
			form(cmd, studentCourseDetails, true)
		} else {
			cmd.apply()
			Mav("related_students/meeting/schedule", "success" -> true, "isIframe" -> true)
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

