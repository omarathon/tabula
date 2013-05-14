package uk.ac.warwick.tabula.profiles.commands

import org.joda.time.DateTimeConstants
import org.joda.time.LocalDate
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.MeetingRecordDao
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.model.MeetingFormat._
import uk.ac.warwick.tabula.data.model.RelationshipType.PersonalTutor
import uk.ac.warwick.tabula.data.model.StaffMember
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.services.ProfileService
import collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import org.junit.Before
import uk.ac.warwick.tabula.data.model.MeetingRecord

class DeleteMeetingRecordCommandTest extends AppContextTestBase with Mockito {

	val someTime = dateTime(2013, DateTimeConstants.APRIL)
	val ps = mock[ProfileService]
	val student = mock[StudentMember]
	var creator: StaffMember = _
	var relationship: StudentRelationship = _
	var meeting: MeetingRecord = _

	val user = mock[CurrentUser]
	user.universityId returns("9876543")

	@Before
	def setUp {
		creator = transactional { tx =>
			val m = new StaffMember("9876543")
			m.userId = "staffmember"
			session.save(m)
			m
		}

		relationship = transactional { tx =>
			val relationship = StudentRelationship("Professor A Tutor", PersonalTutor, "0123456/1")
			relationship.profileService = ps
			ps.getStudentBySprCode("0123456/1") returns (Some(student))

			session.save(relationship)
			relationship
		}

		meeting = transactional { tx =>
			val mr = new MeetingRecord
			mr.creator = creator
			session.save(mr)
			mr
		}
	}

	@Transactional
	@Test
	def testDeleted() {
		var deleted: Boolean = meeting.deleted
		deleted should be (false)

		val cmd = new DeleteMeetingRecordCommand(meeting, user)
		cmd.apply();

		deleted = meeting.deleted
		deleted should be (true)
	}

	@Transactional
	@Test
	def testRestore() {
		meeting.deleted = true

		val cmd = new RestoreMeetingRecordCommand(meeting, user)
		cmd.apply();

		val deleted: Boolean = meeting.deleted
		deleted should be (false)
	}

	@Transactional
	@Test
	def testPurge() {
		meeting.deleted = true
		val id = meeting.id

		val meetingFromSession = session.get(classOf[MeetingRecord], id).asInstanceOf[MeetingRecord]
		meetingFromSession.id should be (id)

		val cmd = new PurgeMeetingRecordCommand(meeting, user)
		cmd.apply();

		val purgedMeeting = session.get(classOf[MeetingRecord], id)
		purgedMeeting should be (null)
	}
}
