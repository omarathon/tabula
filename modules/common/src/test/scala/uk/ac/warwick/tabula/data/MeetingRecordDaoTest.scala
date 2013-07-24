package uk.ac.warwick.tabula.data

import org.joda.time.DateTimeConstants
import org.junit.Before
import uk.ac.warwick.tabula.{PersistenceTestBase, Fixtures}
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.data.model.RelationshipType.PersonalTutor
import uk.ac.warwick.tabula.data.model.StaffMember
import uk.ac.warwick.tabula.data.model.StudentRelationship

// scalastyle:off magic.number
class MeetingRecordDaoTest extends PersistenceTestBase {

	// This test for memberdao maybe shouldn't be involving meetingrecorddao.
	val memberDao = new MemberDaoImpl
	val meetingDao = new MeetingRecordDaoImpl

	@Before
	def setup() {
		memberDao.sessionFactory = sessionFactory
		meetingDao.sessionFactory = sessionFactory
	}

	@Test def createAndList = transactional { tx =>

		val creator = Fixtures.staff(universityId = "0000001", userId="staff1")
		val relationship = StudentRelationship("Professor A Tutor", PersonalTutor, "0123456/1")

		memberDao.saveOrUpdate(creator)
		memberDao.saveOrUpdate(relationship)

		val relSet = Set(relationship)

		val currentMember = new StaffMember
		currentMember.universityId = "0070790"

		meetingDao.list(relSet, currentMember).size should be (0)

		// create some meetings, out of order
		val middleMeeting = new MeetingRecord(creator, relationship)
		val earliestMeeting = new MeetingRecord(creator, relationship)
		val newestMeeting = new MeetingRecord(creator, relationship)

		meetingDao.saveOrUpdate(middleMeeting)
		meetingDao.saveOrUpdate(earliestMeeting)
		meetingDao.saveOrUpdate(newestMeeting)

		middleMeeting.meetingDate = dateTime(2013, DateTimeConstants.APRIL)
		earliestMeeting.meetingDate = dateTime(2013, DateTimeConstants.JANUARY)
		newestMeeting.meetingDate = dateTime(2013, DateTimeConstants.JUNE)

		meetingDao.saveOrUpdate(middleMeeting)
		meetingDao.saveOrUpdate(earliestMeeting)
		meetingDao.saveOrUpdate(newestMeeting)

		val savedMeetings = meetingDao.list(relSet, currentMember)

		savedMeetings.size should be (3)
		savedMeetings.head should be (newestMeeting)
		savedMeetings.last should be (earliestMeeting)
	}
}