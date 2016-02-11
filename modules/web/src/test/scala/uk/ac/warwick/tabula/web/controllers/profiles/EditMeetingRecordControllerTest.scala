package uk.ac.warwick.tabula.web.controllers.profiles

import uk.ac.warwick.tabula.{Mockito, ItemNotFoundException, TestBase}
import uk.ac.warwick.tabula.web.controllers.profiles.relationships.EditMeetingRecordController
import uk.ac.warwick.tabula.data.model.{StudentRelationshipType, StudentMember, StudentRelationship, MeetingRecord}
import uk.ac.warwick.tabula.commands.profiles.EditMeetingRecordCommand
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.MemberStudentRelationship
import uk.ac.warwick.tabula.Fixtures

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
		val relationship = new MemberStudentRelationship
		relationship.relationshipType = new StudentRelationshipType
		relationship.studentMember = Fixtures.student()
		val meeting = new MeetingRecord()
		meeting.relationship = relationship
		// end setup

		controller.getCommand(meeting) should be(anInstanceOf[EditMeetingRecordCommand])
		controller.getCommand(meeting) should not be(null)

	}
}
