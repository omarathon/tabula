package uk.ac.warwick.tabula.profiles.web.controllers

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.profiles.web.controllers.relationships.MeetingRecordModal
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.web.controllers.{ControllerViews, ControllerImports, ControllerMethods}
import uk.ac.warwick.tabula.data.model._

class MeetingRecordModalTest extends TestBase with Mockito {

	// a helper trait that allows for injection of all the stubs/mocks required for constructing a MeetingRecordModal
	trait ModalTestSupport extends  ProfileServiceComponent
	with RelationshipServiceComponent
	with ControllerMethods
	with ControllerImports
	with CurrentMemberComponent
	with ControllerViews
	with MonitoringPointMeetingRelationshipTermServiceComponent {
		var requestInfo: Option[RequestInfo] = _
		var user: CurrentUser = _
		var securityService: SecurityService = mock[SecurityService]
		var optionalCurrentMember: Option[Member] = None
		var currentMember: Member = _
		var profileService: ProfileService = mock[ProfileService]
		var relationshipService: RelationshipService = mock[RelationshipService]
		var monitoringPointMeetingRelationshipTermService = mock[MonitoringPointMeetingRelationshipTermService]
	}

	val student = new StudentCourseDetails()
	student.sprCode = "spr-123"

	@Test
	def viewMeetingRecordCommandUsesRelationshipTypeFromParameter(){
		val modals = new MeetingRecordModal() with ModalTestSupport
		
		val relationshipType1 = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		val relationshipType2 = StudentRelationshipType("supervisor", "supervisor", "research supervisor", "research supervisee")

		modals.viewMeetingRecordCommand(student, relationshipType1) should be('defined)
		modals.viewMeetingRecordCommand(student, relationshipType1).get.relationshipType  should be(relationshipType1)
		modals.viewMeetingRecordCommand(student, relationshipType2).get.relationshipType  should be(relationshipType2)
	}

	@Test
	def allRelationshipsUsesSupervisorRelationshipTypeFromParameter(){
		val modals = new MeetingRecordModal() with ModalTestSupport

		val rels = Seq(new StudentRelationship)
		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		
		modals.relationshipService.findCurrentRelationships(relationshipType, student.sprCode) returns rels

		modals.allRelationships(student, relationshipType) should be(rels)
	}

}
