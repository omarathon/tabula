package uk.ac.warwick.tabula.web.controllers.profiles.relationships

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands.profiles.CreateMeetingRecordCommand
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, _}
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringMeetingRecordServiceComponent
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController

@Controller
@RequestMapping(value = Array("/profiles/{relationshipType}/meeting/{studentCourseDetails}/create"))
class CreateMeetingRecordController extends ProfilesController
	with MeetingRecordModal
	with AutowiringAttendanceMonitoringMeetingRecordServiceComponent
	with AutowiringTermServiceComponent {

	validatesSelf[CreateMeetingRecordCommand]

	@ModelAttribute("command")
	def getCommand(@PathVariable relationshipType: StudentRelationshipType,
				   @PathVariable studentCourseDetails: StudentCourseDetails) =  {
		relationshipService.findCurrentRelationships(mandatory(relationshipType), mandatory(studentCourseDetails)) match {
			case Nil => throw new ItemNotFoundException
			case relationships =>
				// Go through the relationships for this SPR code and find one where the current user is the agent.
				// If there isn't one but there's only one relationship, pick it. Otherwise default to the first.
				val defaultRelationship =
					relationships.find(rel => rel.agentMember.map(_.universityId) == Some(user.universityId))
						.getOrElse(relationships.head)

				new CreateMeetingRecordCommand(currentMember, defaultRelationship, relationships.size > 1)
		}
	}
}
