package uk.ac.warwick.tabula.web.controllers.profiles.relationships.meetings

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.profiles.relationships.meetings._
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, _}
import uk.ac.warwick.tabula.JavaImports._

import scala.collection.JavaConverters._

@Controller
@RequestMapping(value = Array("/profiles/{relationshipType}/meeting/{studentCourseDetails}/{academicYear}/create"))
class CreateMeetingRecordController extends AbstractManageMeetingRecordController with MeetingRecordControllerHelper {

	@ModelAttribute("command")
	def getCommand(
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@ModelAttribute("manageableRelationships") manageableRelationships: Seq[StudentRelationship]
	): Appliable[MeetingRecord] with CreateMeetingRecordCommandState = {
		manageableRelationships match {
			case Nil => throw new ItemNotFoundException
			case relationships =>
				val cmd = CreateMeetingRecordCommand(currentMember, manageableRelationships)
				cmd.relationships = JArrayList(chosenRelationships(relationshipType, relationships, user))
				cmd
		}
	}

}

@Controller
@RequestMapping(value = Array("/profiles/{relationshipType}/meeting/{studentCourseDetails}/{academicYear}/createmissed"))
class CreateMissedMeetingRecordController extends AbstractManageMeetingRecordController with MeetingRecordControllerHelper {

	@ModelAttribute("command")
	def getCommand(
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@ModelAttribute("manageableRelationships") manageableRelationships: Seq[StudentRelationship]
	): Appliable[MeetingRecord] = {
		manageableRelationships match {
			case Nil => throw new ItemNotFoundException
			case relationships =>
				val cmd = CreateMissedMeetingRecordCommand(currentMember, relationships)
				cmd.relationships = JArrayList(chosenRelationships(relationshipType, relationships, user))
				cmd
		}
	}
}
