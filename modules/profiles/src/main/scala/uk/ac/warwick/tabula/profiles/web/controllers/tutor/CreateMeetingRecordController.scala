package uk.ac.warwick.tabula.profiles.web.controllers.tutor

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.RelationshipType._
import uk.ac.warwick.tabula.profiles.commands.CreateMeetingRecordCommand
import scala.Some
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.data.model.StudentCourseDetails

@Controller
@RequestMapping(value = Array("/tutor/meeting/{studentCourseDetails}/create"))
class CreateMeetingRecordController extends ProfilesController with MeetingRecordModal {

	validatesSelf[CreateMeetingRecordCommand]

	@ModelAttribute("command")
	def getCommand(@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails) =  {
		profileService.findCurrentRelationships(PersonalTutor, studentCourseDetails.sprCode) match {
			case Nil => throw new ItemNotFoundException
			case relationships =>
				// Try and guess a default relationship
				val defaultRelationship =
					relationships.find(rel => (rel.agentMember map { _.universityId }) == Some(user.universityId))
					.getOrElse(relationships.head)

				new CreateMeetingRecordCommand(currentMember, defaultRelationship)
		}
	}
}
