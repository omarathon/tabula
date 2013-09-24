package uk.ac.warwick.tabula.profiles.web.controllers.relationships

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.data.model._
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
	def getCommand(@PathVariable("relationshipType") relationshipType: StudentRelationshipType,
				   @PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails) =  {
		relationshipService.findCurrentRelationships(mandatory(relationshipType), studentCourseDetails.sprCode) match {
			case Nil => throw new ItemNotFoundException
			case relationships =>
				// Go through the relationships for this SPR code and find one where the current user is the agent.
				// If there isn't one but there's only one relationship, pick it. Otherwise default to the first.
				val defaultRelationship =
					relationships.find(rel => (rel.agentMember.map(_.universityId)) == Some(user.universityId))
						.getOrElse(relationships.head)

				new CreateMeetingRecordCommand(currentMember, defaultRelationship, relationships.size > 1)
		}
	}
}

