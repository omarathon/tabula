package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.AppContextTestBase
import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import uk.ac.warwick.tabula.data.model.FileAttachment
import java.io.ByteArrayInputStream
import org.joda.time.DateTime
import javax.persistence.Entity
import org.hibernate.annotations.AccessType
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.io.File
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.data.model.Member
import org.junit.Before
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.StaffMember
import org.joda.time.DateTimeConstants
import org.junit.After
import collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.data.model.RelationshipType
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.data.model.RelationshipType.PersonalTutor

class MeetingRecordDaoTest extends AppContextTestBase {

	@Autowired var memberDao: MemberDao =_
	@Autowired var meetingDao :MeetingRecordDao =_
	
	@Test def createAndList = transactional { tx =>
		
		val creator = Fixtures.staff(universityId = "0000001", userId="staff1")
		val relationship = StudentRelationship("Professor A Tutor", PersonalTutor, "0123456/1")
		
		memberDao.saveOrUpdate(creator)
		memberDao.saveOrUpdate(relationship)
		
		val relSet = Set(relationship)
		
		meetingDao.list(relSet).size should be (0)
		
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
		
		val savedMeetings = meetingDao.list(relSet)
		
		savedMeetings.size should be (3)
		savedMeetings.first should be (newestMeeting)
		savedMeetings.last should be (earliestMeeting)
	}
}