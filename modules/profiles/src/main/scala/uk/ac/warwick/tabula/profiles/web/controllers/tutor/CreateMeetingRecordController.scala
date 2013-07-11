package uk.ac.warwick.tabula.profiles.web.controllers.tutor

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.RelationshipType._
import uk.ac.warwick.tabula.profiles.commands.CreateMeetingRecordCommand
import scala.Some
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.ItemNotFoundException


@Controller
@RequestMapping(value = Array("/{relationshipType}/meeting/{studentCourseDetails}/create"))
class CreateMeetingRecordController extends ProfilesController with MeetingRecordModal {

	validatesSelf[CreateMeetingRecordCommand]

	@ModelAttribute("command")
	def getCommand(@PathVariable("relationshipType") relationshipType: RelationshipType, @PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails) =  {
		relationshipService.findCurrentRelationships(relationshipType, studentCourseDetails.sprCode) match {
			case Nil => throw new ItemNotFoundException
			case relationships =>
				// Go through the PersonalTutor relationships for this SPR code and find one where the current user is the agent.
				// If there isn't one but there's only one relationship, pick it.  Otherwise don't try and guess.
				val defaultRelationship =
					relationships.find(rel => (rel.agentMember map {
						_.universityId
					}) == Some(user.universityId))
						.getOrElse(
						if (relationships.size == 1) relationships.head
						else throw new IllegalStateException(
							"Tabula doesn't yet know how to distinguish between multiple tutors to enable third parties to create a meeting record")
					)

				new CreateMeetingRecordCommand(currentMember, defaultRelationship)
		}
	}
}

