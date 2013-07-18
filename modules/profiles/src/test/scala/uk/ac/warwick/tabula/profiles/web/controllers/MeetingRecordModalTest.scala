package uk.ac.warwick.tabula.profiles.web.controllers

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.profiles.web.controllers.tutor.MeetingRecordModal
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.web.controllers.{ControllerViews, ControllerImports, ControllerMethods}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.RelationshipType._

class MeetingRecordModalTest extends TestBase with Mockito {

	// a helper trait that allows for injection of all the stubs/mocks required for constructing a MeetingRecordModal
	trait ModalTestSupport extends  ProfileServiceComponent
	with RelationshipServiceComponent
	with ControllerMethods
	with ControllerImports
	with CurrentMemberComponent
	with ControllerViews{
		var requestInfo: Option[RequestInfo] = _
		var user: CurrentUser = _
		var securityService: SecurityService = mock[SecurityService]
		var currentMember:Member = _
		var profileService: ProfileService = mock[ProfileService]
		var relationshipService:RelationshipService = mock[RelationshipService]
	}

	val student = new StudentCourseDetails()
	student.sprCode = "spr-123"

	@Test
	def viewMeetingRecordCommandUsesRelationshipTypeFromParameter(){
		val modals = new MeetingRecordModal() with ModalTestSupport

		modals.viewMeetingRecordCommand(student, Supervisor) should be('defined)
		modals.viewMeetingRecordCommand(student,Supervisor).get.relationshipType  should be(Supervisor)
		modals.viewMeetingRecordCommand(student,PersonalTutor).get.relationshipType  should be(PersonalTutor)
	}

	@Test
	def allRelationshipsUsesSupervisorRelationshipTypeFromParameter(){
		val modals = new MeetingRecordModal() with ModalTestSupport

		val rels = Seq(new StudentRelationship)
		modals.relationshipService.findCurrentRelationships(Supervisor,student.sprCode) returns rels

		modals.allRelationships(student, Supervisor) should be(rels)
	}

	@Test
	def allRelationshipsUsesTutorRelationshipTypeFromParameter(){
		val modals = new MeetingRecordModal() with ModalTestSupport

		val rels = Seq(new StudentRelationship)
		modals.relationshipService.findCurrentRelationships(PersonalTutor,student.sprCode) returns rels

		modals.allRelationships(student, PersonalTutor) should be(rels)
	}

}
