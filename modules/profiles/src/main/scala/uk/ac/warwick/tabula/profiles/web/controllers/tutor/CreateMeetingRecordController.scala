package uk.ac.warwick.tabula.profiles.web.controllers.tutor

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.RelationshipType._
import uk.ac.warwick.tabula.profiles.commands.CreateMeetingRecordCommand

import scala.Some
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController

@Controller
@RequestMapping(value = Array("/tutor/meeting/{student}/create"))
class CreateMeetingRecordController extends ProfilesController with MeetingRecordModal {

	validatesSelf[CreateMeetingRecordCommand]

	@ModelAttribute("command")
	def getCommand(@PathVariable("student") member: Member) = member match {
		case student: StudentMember => {
			profileService.findCurrentRelationships(PersonalTutor, student.studyDetails.sprCode) match {
				case Nil => throw new ItemNotFoundException
				case relationships => 
					// Try and guess a default relationship
					val defaultRelationship = 
						relationships.find(rel => (rel.agentMember map { _.universityId }) == Some(user.universityId))
						.getOrElse(relationships.head)
						
					new CreateMeetingRecordCommand(currentMember, defaultRelationship)
			}
		}
		case _ => throw new ItemNotFoundException
	}
}
