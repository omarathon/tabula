package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import org.joda.time.DateTimeConstants
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.RelationshipType.PersonalTutor

class MeetingRecordTest extends TestBase with Mockito {
	
	val aprilFool = dateTime(2013, DateTimeConstants.APRIL)
	
	@Test def defaultConstructor = withFakeTime(aprilFool) {
		val meeting = new MeetingRecord
		
		meeting.creationDate should be (aprilFool)
		meeting.lastUpdatedDate should be (aprilFool)
		
		meeting.creator should be (null)
		meeting.relationship should be (null)
		meeting.meetingDate should be (null)
		meeting.format should be (null)
		meeting should be ('approved)
	}
	
	@Test def everydayConstructor = withFakeTime(aprilFool) {
		val creator = new StaffMember
		val relationship = StudentRelationship("Professor A Tutor", PersonalTutor, "0123456/1")
		
		val meeting = new MeetingRecord(creator, relationship)
		
		meeting.creationDate should be (aprilFool)
		meeting.lastUpdatedDate should be (aprilFool)
		
		meeting.creator should be (creator)
		meeting.relationship should be (relationship)
		meeting.meetingDate should be (null)
		meeting.format should be (null)
		meeting should be ('approved)
	}
}