package uk.ac.warwick.tabula.data.model

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula.{Fixtures, PersistenceTestBase}

import scala.collection.JavaConverters._

// scalastyle:off magic.number
class MeetingRecordTest extends PersistenceTestBase {

	val aprilFool: DateTime = dateTime(2013, DateTimeConstants.APRIL)

	@Test def deleteFileAttachmentOnDelete(): Unit = transactional {ts=>
		val orphanAttachment = flushing(session) {
			val attachment = new FileAttachment
			session.save(attachment)
			attachment
		}

		val (creator, relationship) = flushing(session){
			val creator = new StaffMember(id = idFormat(1))
			creator.userId = idFormat(11)
			val relationship = new ExternalStudentRelationship
			session.save(creator)
			session.save(relationship)
			(creator, relationship)
		}

		val (meetingRecord, meetingRecordkAttachment) = flushing(session) {
			val meetingRecord = new MeetingRecord(creator, Seq(relationship))
			meetingRecord.id = idFormat(2)

			val attachment = new FileAttachment
			meetingRecord.attachments = List(attachment).asJava

			session.save(meetingRecord)
			(meetingRecord, attachment)
		}

		// Ensure everything's been persisted
		orphanAttachment.id should not be null
		meetingRecord.id should not be null
		meetingRecordkAttachment.id should not be null

		// Can fetch everything from db
		flushing(session) {
			session.get(classOf[FileAttachment], orphanAttachment.id) should be (orphanAttachment)
			session.get(classOf[MeetingRecord], meetingRecord.id) should be (meetingRecord)
			session.get(classOf[FileAttachment], meetingRecordkAttachment.id) should be (meetingRecordkAttachment)
		}

		flushing(session) { session.delete(meetingRecord) }

		// Ensure we can't fetch the feedback or attachment, but all the other objects are returned
		flushing(session) {
			session.get(classOf[FileAttachment], orphanAttachment.id) should be (orphanAttachment)
			session.get(classOf[MeetingRecord], meetingRecord.id) should be (null)
			session.get(classOf[FileAttachment], meetingRecordkAttachment.id) should be (null)
		}
	}



	@Test def defaultConstructor() = withFakeTime(aprilFool) {
		val meeting = new MeetingRecord

		meeting.creationDate should be (aprilFool)
		meeting.lastUpdatedDate should be (aprilFool)

		meeting.creator should be (null)
		meeting.relationships should be (empty)
		meeting.meetingDate should be (null)
		meeting.format should be (null)
		meeting should be ('approved)
	}

	@Test def everydayConstructor() = withFakeTime(aprilFool) {
		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

		val student = Fixtures.student(universityId = "1000001", userId="student")

		val creator = new StaffMember
		val relationship = ExternalStudentRelationship("Professor A Tutor", relationshipType, student, DateTime.now)

		val meeting = new MeetingRecord(creator, Seq(relationship))

		meeting.creationDate should be (aprilFool)
		meeting.lastUpdatedDate should be (aprilFool)

		meeting.creator should be (creator)
		meeting.relationships should contain only relationship
		meeting.meetingDate should be (null)
		meeting.format should be (null)
		meeting should be ('approved)
	}

	@Test def relationships(): Unit = {
		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

		val student = Fixtures.student(universityId = "1000001", userId="student")

		val creator = new StaffMember
		val relationship = ExternalStudentRelationship("Professor A Tutor", relationshipType, student, DateTime.now)

		val meeting = new MeetingRecord
		meeting.creator = creator

		meeting.relationships should be (empty)

		meeting.relationship = relationship
		meeting.relationships should contain only relationship

		meeting.relationships = Seq(relationship)
		meeting.relationship should be (null)
		meeting.relationships should contain only relationship
	}

	@Test def replaceParticipant(): Unit = {
		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

		val student = Fixtures.student(universityId = "1000001", userId="student")

		val creator = new StaffMember
		val original = ExternalStudentRelationship("Professor A Tutor", relationshipType, student, DateTime.now)
		val replacement = ExternalStudentRelationship("Professor B Tutor", relationshipType, student, DateTime.now)

		val meeting = new MeetingRecord(creator, Seq(original))

		meeting.replaceParticipant(original, replacement)
		meeting.relationships should contain only replacement
	}

	@Test def people(): Unit = {
		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

		val student = Fixtures.student(universityId = "1000001", userId="student")
		student.firstName = "Student"
		student.lastName = "Member"

		val agent = Fixtures.staff()
		agent.firstName = "Staff"
		agent.lastName = "Member"

		val relationship = StudentRelationship(agent, relationshipType, student, DateTime.now)

		val meeting = new MeetingRecord(agent, Seq(relationship))

		meeting.student should be (student)
		meeting.agents should contain only agent
		meeting.participants should contain allOf (student, agent)
		meeting.allParticipantNames should be ("Staff Member and Student Member")
		meeting.allAgentNames should be ("Staff Member")
	}

	/** Zero-pad integer to a 7 digit string */
	def idFormat(i:Int): String = "%07d" format i
}