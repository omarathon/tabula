package uk.ac.warwick.tabula.profiles.web.controllers.tutor

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{RelationshipService, ProfileService}
import org.mockito.Mockito._

class CreateMeetingRecordControllerTest extends TestBase with Mockito {

	val student = new StudentMember()
	val studentCourseDetails = new StudentCourseDetails()
	studentCourseDetails.sprCode = "sprcode"
	studentCourseDetails.student = student

	val controller = new CreateMeetingRecordController

	val profileService = mock[ProfileService]
	controller.profileService = profileService
	val relationshipService  = mock[RelationshipService]
	controller.relationshipService = relationshipService

	when(profileService.getMemberByUserId("tutor", true)).thenReturn(None)
	when(profileService.getMemberByUserId("supervisor", true)).thenReturn(None)
	when(profileService.getStudentBySprCode(studentCourseDetails.sprCode)).thenReturn(Some(student))

	@Test
	def passesTutorRelationshipTypeToCommand() {
		withUser("tutor") {

			val relationship = new StudentRelationship
			relationship.targetSprCode = studentCourseDetails.sprCode
			relationship.relationshipType = RelationshipType.PersonalTutor
			relationship.profileService = profileService
			when(relationshipService.findCurrentRelationships(RelationshipType.PersonalTutor, studentCourseDetails.sprCode)).thenReturn(Seq(relationship))
			relationship.profileService = profileService

			val tutorCommand = controller.getCommand(RelationshipType.PersonalTutor, studentCourseDetails)
			tutorCommand.relationship.relationshipType should be(RelationshipType.PersonalTutor)
		}
	}

	@Test
	def passesSupervisorRelationshipTypeToCommand() {
		withUser("supervisor") {
			val relationship = new StudentRelationship
			relationship.targetSprCode = studentCourseDetails.sprCode
			relationship.relationshipType = RelationshipType.Supervisor
			when(relationshipService.findCurrentRelationships(RelationshipType.Supervisor, studentCourseDetails.sprCode)).thenReturn(Seq(relationship))
			relationship.profileService = profileService

			studentCourseDetails.relationshipService = relationshipService
			controller.profileService = profileService
			relationship.profileService = profileService

			val supervisorCommand = controller.getCommand(RelationshipType.Supervisor, studentCourseDetails)
			supervisorCommand.relationship.relationshipType should be(RelationshipType.Supervisor)

		}
	}

}
