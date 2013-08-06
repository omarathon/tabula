package uk.ac.warwick.tabula.profiles.web.controllers.tutor

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{RelationshipService, ProfileService}
import org.mockito.Mockito._
import uk.ac.warwick.tabula.ItemNotFoundException

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

	@Test(expected=classOf[ItemNotFoundException])
	def throwsWithoutRelationships() {
		withUser("tutor") {
			when(relationshipService.findCurrentRelationships(RelationshipType.PersonalTutor, studentCourseDetails.sprCode)).thenReturn(Nil)

			val tutorCommand = controller.getCommand(RelationshipType.PersonalTutor, studentCourseDetails)
		}
	}

	@Test
	def passesTutorRelationshipTypeToCommand() {
		withUser("tutor") {
			val relationship = new StudentRelationship
			relationship.targetSprCode = studentCourseDetails.sprCode
			relationship.relationshipType = RelationshipType.PersonalTutor
			relationship.profileService = profileService
			when(relationshipService.findCurrentRelationships(RelationshipType.PersonalTutor, studentCourseDetails.sprCode)).thenReturn(Seq(relationship))

			val tutorCommand = controller.getCommand(RelationshipType.PersonalTutor, studentCourseDetails)
			tutorCommand.relationship.relationshipType should be(RelationshipType.PersonalTutor)
			tutorCommand.considerAlternatives should be(false)
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

			val supervisorCommand = controller.getCommand(RelationshipType.Supervisor, studentCourseDetails)
			supervisorCommand.relationship.relationshipType should be(RelationshipType.Supervisor)
			supervisorCommand.considerAlternatives should be(false)
		}
	}


	@Test
	def passesFirstRelationshipTypeToCommand() {
		val uniId = "1234765"
		withUser("supervisor", uniId) {
			val firstAgent = "first"
			// use non-numeric agent in test to avoid unecessary member lookup
			val rel1 = new StudentRelationship
			rel1.targetSprCode = studentCourseDetails.sprCode
			rel1.relationshipType = RelationshipType.Supervisor
			rel1.agent = firstAgent
			rel1.profileService = profileService
			val secondAgent = "second"
			val rel2 = new StudentRelationship
			rel2.targetSprCode = studentCourseDetails.sprCode
			rel2.relationshipType = RelationshipType.Supervisor
			rel2.agent = secondAgent
			rel2.profileService = profileService

			when(relationshipService.findCurrentRelationships(RelationshipType.Supervisor, studentCourseDetails.sprCode)).thenReturn(Seq(rel1, rel2))

			studentCourseDetails.relationshipService = relationshipService
			controller.profileService = profileService

			val supervisorCommand = controller.getCommand(RelationshipType.Supervisor, studentCourseDetails)
			supervisorCommand.relationship.relationshipType should be(RelationshipType.Supervisor)
			supervisorCommand.relationship.agent should be(firstAgent)
			supervisorCommand.considerAlternatives should be(true)
			supervisorCommand.creator.universityId should be(uniId)
		}
	}

}
