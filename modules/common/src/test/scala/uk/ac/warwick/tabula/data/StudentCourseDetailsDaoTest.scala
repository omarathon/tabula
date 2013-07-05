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
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.model.StudentCourseDetails

class StudentCourseDetailsDaoTest extends AppContextTestBase with Logging {
	@Autowired var memberDao:MemberDao =_
	@Autowired var dao:StudentCourseDetailsDao =_

	@Before def setup: Unit = transactional { tx =>
		session.enableFilter(Member.ActiveOnlyFilter)
	}

	@After def tidyUp: Unit = transactional { tx =>
		session.disableFilter(Member.ActiveOnlyFilter)

		session.createCriteria(classOf[Member]).list().asInstanceOf[JList[Member]].asScala map { session.delete(_) }
	}


	@Test def getByScjCode = transactional { tx =>
		val dept1 = Fixtures.department("ms", "Motorsport")
		val dept2 = Fixtures.department("vr", "Vehicle Repair")

		session.save(dept1)
		session.save(dept2)

		session.flush

		val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1)
		stu1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val stu2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2)
		stu2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		memberDao.saveOrUpdate(stu1)
		memberDao.saveOrUpdate(stu2)

		session.flush()

		dao.getByScjCode("1000001/1").get.department should be (dept1)
		dao.getByScjCode("1000001/1").get.student.universityId should be ("1000001")
		dao.getStudentBySprCode("1000001/1").get.universityId should be ("1000001")

		session.delete(dept1)
		session.delete(dept2)
		session.delete(stu1)
		session.delete(stu2)

		session.flush()
	}


}
