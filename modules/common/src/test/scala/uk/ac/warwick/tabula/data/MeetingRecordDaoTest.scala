package uk.ac.warwick.tabula.data

import scala.collection.JavaConverters._
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import org.junit.Test
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.data.model.RelationshipType
import uk.ac.warwick.tabula.data.model.RelationshipType.PersonalTutor
import uk.ac.warwick.tabula.data.model.StaffMember
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.data.model.Member

// scalastyle:off magic.number
class MeetingRecordDaoTest extends AppContextTestBase {

	@Autowired var memberDao: MemberDao =_
	@Autowired var meetingDao :MeetingRecordDao =_

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