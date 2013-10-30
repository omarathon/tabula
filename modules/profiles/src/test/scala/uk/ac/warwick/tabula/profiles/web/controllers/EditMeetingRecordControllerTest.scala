package uk.ac.warwick.tabula.profiles.web.controllers

import uk.ac.warwick.tabula.{Mockito, ItemNotFoundException, TestBase}
import uk.ac.warwick.tabula.profiles.web.controllers.relationships.EditMeetingRecordController
import uk.ac.warwick.tabula.data.model.{StudentRelationshipType, StudentMember, StudentRelationship, MeetingRecord}
import uk.ac.warwick.tabula.profiles.commands.EditMeetingRecordCommand
import uk.ac.warwick.tabula.services.ProfileService

class EditMeetingRecordControllerTest extends TestBase with Mockito{

	@Test
	def nullMeetingRecordIsAbsolutelyNotOK() {
		val controller = new EditMeetingRecordController()
		intercept[ItemNotFoundException] {
			controller.getCommand(null)
			fail
		}
	}

	@Test
	def returnsAppropriateCommand(){
		val controller = new EditMeetingRecordController()

		// faffy setup to keep the permissions checking happy
		val relationship = new StudentRelationship
		relationship.profileService = mock[ProfileService]
		relationship.profileService.getStudentBySprCode(any[String]) returns Some(new StudentMember("test"))
		relationship.relationshipType = new StudentRelationshipType
		val meeting = new MeetingRecord()
		meeting.relationship = relationship
		// end setup

		controller.getCommand(meeting) should be(anInstanceOf[EditMeetingRecordCommand])
		controller.getCommand(meeting) should not be(null)

	}
}
