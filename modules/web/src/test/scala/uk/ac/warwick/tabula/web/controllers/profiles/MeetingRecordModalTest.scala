package uk.ac.warwick.tabula.web.controllers.profiles

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.web.controllers.profiles.relationships.MeetingRecordModal
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringMeetingRecordServiceComponent, AttendanceMonitoringMeetingRecordService}
import uk.ac.warwick.tabula.web.controllers.{ControllerViews, ControllerImports, ControllerMethods}
import uk.ac.warwick.tabula.data.model._

class MeetingRecordModalTest extends TestBase with Mockito {

	// a helper trait that allows for injection of all the stubs/mocks required for constructing a MeetingRecordModal
	trait ModalTestSupport extends ProfileServiceComponent
	with RelationshipServiceComponent
	with ControllerMethods
	with ControllerImports
	with CurrentMemberComponent
	with ControllerViews
	with AttendanceMonitoringMeetingRecordServiceComponent
	with TermServiceComponent {
		var requestInfo: Option[RequestInfo] = _
		var user: CurrentUser = _
		var securityService: SecurityService = smartMock[SecurityService]
		var optionalCurrentMember: Option[Member] = None
		var currentMember: Member = _
		var profileService: ProfileService = smartMock[ProfileService]
		var relationshipService: RelationshipService = smartMock[RelationshipService]
		val attendanceMonitoringMeetingRecordService = smartMock[AttendanceMonitoringMeetingRecordService]
		val termService = smartMock[TermService]
	}

	val scd = new StudentCourseDetails()
	scd.sprCode = "spr-123"

	@Test
	def viewMeetingRecordCommandUsesRelationshipTypeFromParameter(){
		val modals = new MeetingRecordModal() with ModalTestSupport

		val relationshipType1 = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		val relationshipType2 = StudentRelationshipType("supervisor", "supervisor", "research supervisor", "research supervisee")

		modals.viewMeetingRecordCommand(scd, relationshipType1) should be('defined)
		modals.viewMeetingRecordCommand(scd, relationshipType1).get.relationshipType  should be(relationshipType1)
		modals.viewMeetingRecordCommand(scd, relationshipType2).get.relationshipType  should be(relationshipType2)
	}

	@Test
	def allRelationshipsUsesSupervisorRelationshipTypeFromParameter(){
		val modals = new MeetingRecordModal() with ModalTestSupport

		val rels = Seq(new MemberStudentRelationship)
		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

		modals.relationshipService.findCurrentRelationships(relationshipType, scd) returns rels

		modals.allRelationships(scd, relationshipType) should be(rels)
	}

}
