package uk.ac.warwick.tabula.profiles.services.timetables

import uk.ac.warwick.tabula.{Fixtures, CurrentUser, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.{AbstractMeetingRecord, StudentRelationshipType, StudentRelationship, StudentMember}
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEventType}
import org.joda.time.LocalDateTime
import uk.ac.warwick.tabula.services.{SecurityServiceComponent, SecurityService, MeetingRecordServiceComponent, RelationshipServiceComponent, MeetingRecordService, RelationshipService}
import uk.ac.warwick.tabula.permissions.Permissions

class MeetingRecordServiceScheduledMeetingEventSourceComponentTest extends TestBase with Mockito {
	val student = new StudentMember
	student.universityId = "university ID"

	val user = mock[CurrentUser]
	user.profile returns (Some(student))

	val occurrence = EventOccurrence("", "", TimetableEventType.Meeting, LocalDateTime.now, LocalDateTime.now, None, None, Nil)

	val relationshipType = StudentRelationshipType("t", "t", "t", "t")
	val relationships = Seq(StudentRelationship(Fixtures.staff(), relationshipType, student))
	val meetings = Seq(new AbstractMeetingRecord {
		relationship = relationships.head

		override def toEventOccurrence = Some(occurrence)
	})

	val source = new MeetingRecordServiceScheduledMeetingEventSourceComponent
		with RelationshipServiceComponent
		with MeetingRecordServiceComponent
		with SecurityServiceComponent {

		val relationshipService = mock[RelationshipService]
		val meetingRecordService = mock[MeetingRecordService]
		val securityService = mock[SecurityService]

	}

	source.relationshipService.getAllPastAndPresentRelationships(student) returns relationships
	source.securityService.can(user, Permissions.Profiles.MeetingRecord.Read(relationshipType), student) returns (true)
	source.meetingRecordService.listAll(relationships.toSet, student) returns meetings

	@Test
	def callsBothServicesAndGeneratesOccurrence(){
		source.scheduledMeetingEventSource.occurrencesFor(student, user) should be (Seq(occurrence))
	}


}