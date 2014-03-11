package uk.ac.warwick.tabula.profiles.services.timetables

import uk.ac.warwick.tabula.{Fixtures, CurrentUser, AcademicYear, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.{AbstractMeetingRecord, StudentRelationshipType, StudentRelationship, StudentMember}
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEventType, TimetableEvent}
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import org.joda.time.{LocalDateTime, LocalTime}
import uk.ac.warwick.tabula.services.{MeetingRecordServiceComponent, RelationshipServiceComponent, MeetingRecordService, RelationshipService}

class MeetingRecordServiceScheduledMeetingEventSourceComponentTest extends TestBase with Mockito {
	val student = new StudentMember
	student.universityId = "university ID"

	val user = mock[CurrentUser]
	user.profile returns (Some(student))

	val occurrence = EventOccurrence("", "", TimetableEventType.Meeting, LocalDateTime.now, LocalDateTime.now, None, None, Nil)

	val relationships = Seq(StudentRelationship(Fixtures.staff(), StudentRelationshipType("t", "t", "t", "t"), student))
	val meetings = Seq(new AbstractMeetingRecord {
		override def toEventOccurrence = Some(occurrence)
	})

	val source = new MeetingRecordServiceScheduledMeetingEventSourceComponent
		with RelationshipServiceComponent
		with MeetingRecordServiceComponent {

		val relationshipService = mock[RelationshipService]
		val meetingRecordService = mock[MeetingRecordService]

	}

	source.relationshipService.getAllPastAndPresentRelationships(student) returns relationships
	source.meetingRecordService.listAll(relationships.toSet, student) returns meetings

	@Test
	def callsBothServicesAndGeneratesOccurrence(){
		source.scheduledMeetingEventSource.occurrencesFor(student, user) should be (Seq(occurrence))
	}


}