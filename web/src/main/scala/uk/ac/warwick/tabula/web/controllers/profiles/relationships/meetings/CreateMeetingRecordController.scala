package uk.ac.warwick.tabula.web.controllers.profiles.relationships.meetings

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.profiles.relationships.meetings._
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, _}

@Controller
@RequestMapping(value = Array("/profiles/{relationshipType}/meeting/{studentCourseDetails}/{academicYear}/create"))
class CreateMeetingRecordController extends AbstractManageMeetingRecordController {

	@ModelAttribute("command")
	def getCommand(
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@RequestParam(value = "relationship", required = false) relationship: StudentRelationship,
		@ModelAttribute("allRelationships") allRelationships: Seq[StudentRelationship]
	): Appliable[MeetingRecord] = {
		allRelationships match {
			case Nil => throw new ItemNotFoundException
			case relationships =>
				// Go through the relationships for this SPR code and find one where the current user is the agent.
				// If there isn't one but there's only one relationship, pick it. Otherwise default to the first.
				val chosenRelationship = relationship match {
					case r: StudentRelationship => r
					case _ => relationships.find(rel => rel.agentMember.map(_.universityId).contains(user.universityId))
						.getOrElse(relationships.head)
				}

				CreateMeetingRecordCommand(currentMember, chosenRelationship)
		}
	}

}

@Controller
@RequestMapping(value = Array("/profiles/{relationshipType}/meeting/{studentCourseDetails}/{academicYear}/createmissed"))
class CreateMissedMeetingRecordController extends AbstractManageMeetingRecordController {

	@ModelAttribute("missedMeeting")
	def missedMeeting: Boolean = true

	@ModelAttribute("command")
	def getCommand(
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@RequestParam(value = "relationship", required = false) relationship: StudentRelationship,
		@ModelAttribute("allRelationships") allRelationships: Seq[StudentRelationship]
	): Appliable[MeetingRecord] = {
		allRelationships match {
			case Nil => throw new ItemNotFoundException
			case relationships =>
				// Go through the relationships for this SPR code and find one where the current user is the agent.
				// If there isn't one but there's only one relationship, pick it. Otherwise default to the first.
				val chosenRelationship = relationship match {
					case r: StudentRelationship => r
					case _ => relationships.find(rel => rel.agentMember.map(_.universityId).contains(user.universityId))
						.getOrElse(relationships.head)
				}

				CreateMissedMeetingRecordCommand(currentMember, chosenRelationship)
		}
	}

}
