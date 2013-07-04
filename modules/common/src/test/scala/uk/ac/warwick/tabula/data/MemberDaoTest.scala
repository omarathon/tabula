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

// scalastyle:off magic.number
class MemberDaoTest extends AppContextTestBase with Logging {

	@Autowired var dao:MemberDao =_

	@Before def setup: Unit = transactional { tx =>
		session.enableFilter(Member.ActiveOnlyFilter)
	}

	@After def tidyUp: Unit = transactional { tx =>
		session.disableFilter(Member.ActiveOnlyFilter)

		session.createCriteria(classOf[Member]).list().asInstanceOf[JList[Member]].asScala map { session.delete(_) }
	}

	@Test def crud = transactional { tx =>
		val m1 = Fixtures.student(universityId = "0000001", userId="student")
		val m2 = Fixtures.student(universityId = "0000002", userId="student")

		val m3 = Fixtures.staff(universityId = "0000003", userId="staff1")
		val m4 = Fixtures.staff(universityId = "0000004", userId="staff2")

		dao.saveOrUpdate(m1)
		dao.saveOrUpdate(m2)
		dao.saveOrUpdate(m3)
		dao.saveOrUpdate(m4)

		dao.saveOrUpdate(m1)
		dao.saveOrUpdate(m2)

		dao.getByUniversityId("0000001") should be (Some(m1))
		dao.getByUniversityId("0000002") should be (Some(m2))
		dao.getByUniversityId("0000003") should be (Some(m3))
		dao.getByUniversityId("0000004") should be (Some(m4))
		dao.getByUniversityId("0000005") should be (None)

		session.enableFilter(Member.StudentsOnlyFilter)

		dao.getByUniversityId("0000003") should be (None)
		dao.getByUniversityId("0000004") should be (None)

		dao.getAllByUserId("student", false) should be (Seq(m1, m2))
		dao.getByUserId("student", false) should be (Some(m1))
		dao.getAllByUserId("student", true) should be (Seq(m1, m2))
		dao.getAllByUserId("staff1", false) should be (Seq())
		dao.getByUserId("staff1", false) should be (None)
		dao.getAllByUserId("staff1", true) should be (Seq(m3))
		dao.getByUserId("staff1", true) should be (Some(m3))
		dao.getAllByUserId("unknown", false) should be (Seq())
		dao.getAllByUserId("unknown", true) should be (Seq())

		session.disableFilter(Member.StudentsOnlyFilter)

		dao.getAllByUserId("staff1", false) should be (Seq(m3))
		dao.getByUserId("staff1", false) should be (Some(m3))
	}

	@Test def listUpdatedSince = transactional { tx =>
		val dept1 = Fixtures.department("hi", "History")
		val dept2 = Fixtures.department("fr", "French")

		session.save(dept1)
		session.save(dept2)

		session.flush

		val m1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1)
		m1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val m2 = Fixtures.student(universityId = "1000002", userId="student", department=dept1)
		m2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val m3 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		m3.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val m4 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		m4.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		dao.saveOrUpdate(m1)
		dao.saveOrUpdate(m2)
		dao.saveOrUpdate(m3)
		dao.saveOrUpdate(m4)

		session.flush

		dao.listUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), 5) should be (Seq(m1, m2, m3, m4))
		dao.listUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), 1) should be (Seq(m1))
		dao.listUpdatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 0, 0, 0, 0), 5) should be (Seq(m2, m3, m4))
		dao.listUpdatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 5, 0, 0, 0, 0), 5) should be (Seq())

		dao.listUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), dept1, 5) should be (Seq(m1, m2, m3))
		dao.listUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), dept1, 1) should be (Seq(m1))
		dao.listUpdatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 0, 0, 0, 0), dept1, 5) should be (Seq(m2, m3))
		dao.listUpdatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 5, 0, 0, 0, 0), dept1, 5) should be (Seq())
		dao.listUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), dept2, 5) should be (Seq(m4))
	}

	@Test def getRegisteredModules: Unit = transactional { tx =>
		val mod1 = Fixtures.module("in101")
		val mod2 = Fixtures.module("in102")
		val mod3 = Fixtures.module("in103")

		session.save(mod1)
		session.save(mod2)
		session.save(mod3)

		val ua1 = Fixtures.upstreamAssignment("in", 1)
		val ua2 = Fixtures.upstreamAssignment("in", 2)

		// Check that we haven't changed the behaviour of Fixtures
		ua1.moduleCode should startWith (mod1.code.toUpperCase())
		ua2.moduleCode should startWith (mod2.code.toUpperCase())

		session.save(ua1)
		session.save(ua2)

		val ag1 = Fixtures.assessmentGroup(ua1)
		val ag2 = Fixtures.assessmentGroup(ua2)

		session.save(ag1)
		session.save(ag2)

		ag1.members.staticIncludeUsers.add("0000001")
		ag1.members.staticIncludeUsers.add("0000002")

		ag2.members.staticIncludeUsers.add("0000002")
		ag2.members.staticIncludeUsers.add("0000003")

		session.update(ag1)
		session.update(ag2)

		session.flush

		dao.getRegisteredModules("0000001") should be (Seq(mod1))
		dao.getRegisteredModules("0000002").toSet should be (Seq(mod1, mod2).toSet)
		dao.getRegisteredModules("0000003") should be (Seq(mod2))
		dao.getRegisteredModules("0000004") should be (Seq())
	}

	@Test def studentRelationshipsCurrentAndByTarget = transactional { tx =>
		val dept1 = Fixtures.department("sp", "Spanish")
		val dept2 = Fixtures.department("en", "English")

		session.save(dept1)
		session.save(dept2)

		session.flush

		val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1)
		stu1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val stu2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept1)
		stu2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val staff1 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		staff1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val staff2 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		staff2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		dao.saveOrUpdate(stu1)
		dao.saveOrUpdate(stu2)
		dao.saveOrUpdate(staff1)
		dao.saveOrUpdate(staff2)

		val relBetweenStaff1AndStu1 = StudentRelationship("1000003", RelationshipType.PersonalTutor, "1000001/1")
		val relBetweenStaff1AndStu2 = StudentRelationship("1000003", RelationshipType.PersonalTutor, "1000002/1")

		dao.saveOrUpdate(relBetweenStaff1AndStu1)
		dao.saveOrUpdate(relBetweenStaff1AndStu2)
		session.flush()

		dao.getCurrentRelationships(RelationshipType.PersonalTutor, "1000001/1") should be (Seq(relBetweenStaff1AndStu1))
		dao.getCurrentRelationships(RelationshipType.PersonalTutor, "1000002/1") should be (Seq(relBetweenStaff1AndStu2))
		dao.getCurrentRelationships(RelationshipType.PersonalTutor, "1000003/1") should be (Nil)
		dao.getCurrentRelationships(null, "1000001/1") should be (Nil)

		dao.getRelationshipsByTarget(RelationshipType.PersonalTutor, "1000001/1") should be (Seq(relBetweenStaff1AndStu1))
		dao.getRelationshipsByTarget(RelationshipType.PersonalTutor, "1000002/1") should be (Seq(relBetweenStaff1AndStu2))
		dao.getRelationshipsByTarget(RelationshipType.PersonalTutor, "1000003/1") should be (Seq())
		dao.getRelationshipsByTarget(null, "1000001/1") should be (Seq())

		session.delete(relBetweenStaff1AndStu1)
		session.delete(relBetweenStaff1AndStu2)
		session.flush()
	}

	@Test def studentRelationshipsByDepartmentAndAgent = transactional { tx =>
		val dept1 = Fixtures.department("hm", "History of Music")
		val dept2 = Fixtures.department("ar", "Architecture")

		session.save(dept1)
		session.save(dept2)

		session.flush

		val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1)
		stu1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val stu2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2)
		stu2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val staff1 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		staff1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val staff2 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		staff2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		dao.saveOrUpdate(stu1)
		dao.saveOrUpdate(stu2)
		dao.saveOrUpdate(staff1)
		dao.saveOrUpdate(staff2)

		val relBetweenStaff1AndStu1 = StudentRelationship("1000003", RelationshipType.PersonalTutor, "1000001/1")
		val relBetweenStaff1AndStu2 = StudentRelationship("1000003", RelationshipType.PersonalTutor, "1000002/1")

		dao.saveOrUpdate(relBetweenStaff1AndStu1)
		dao.saveOrUpdate(relBetweenStaff1AndStu2)
		session.flush()


		val ret = dao.getRelationshipsByDepartment(RelationshipType.PersonalTutor, dept1)
		ret(0).studentMember.get.universityId should be ("1000001")
		ret(0).studentMember.get.mostSignificantCourseDetails.get.department.code should be ("hm")

		dao.getRelationshipsByDepartment(RelationshipType.PersonalTutor, dept1) should be (Seq(relBetweenStaff1AndStu1))
		dao.getRelationshipsByDepartment(RelationshipType.PersonalTutor, dept2) should be (Seq(relBetweenStaff1AndStu2))
		dao.getRelationshipsByDepartment(null, dept1) should be (Seq())

		dao.getRelationshipsByAgent(RelationshipType.PersonalTutor, "1000003").toSet should be (Seq(relBetweenStaff1AndStu1, relBetweenStaff1AndStu2).toSet)
		dao.getRelationshipsByAgent(RelationshipType.PersonalTutor, "1000004") should be (Seq())
		dao.getRelationshipsByAgent(null, "1000003") should be (Seq())

		session.delete(relBetweenStaff1AndStu1)
		session.delete(relBetweenStaff1AndStu2)
		session.flush()

	}

	@Test def studentsWithoutRelationships = transactional { tx =>
		val dept1 = Fixtures.department("af", "Art of Foraging")
		val dept2 = Fixtures.department("tm", "Traditional Music")

		session.save(dept1)
		session.save(dept2)

		session.flush

		val relBetweenStaff1AndStu1 = StudentRelationship("1000003", RelationshipType.PersonalTutor, "1000001/1")
		val relBetweenStaff1AndStu2 = StudentRelationship("1000003", RelationshipType.PersonalTutor, "1000002/1")

		dao.saveOrUpdate(relBetweenStaff1AndStu1)
		dao.saveOrUpdate(relBetweenStaff1AndStu2)
		session.flush()

		val m5 = Fixtures.student(universityId = "1000005", userId="student", department=dept1, courseDepartment=dept1)
		val m6 = Fixtures.student(universityId = "1000006", userId="student", department=dept2, courseDepartment=dept2)

		dao.saveOrUpdate(m5)
		dao.saveOrUpdate(m6)

		dao.getStudentsWithoutRelationshipByDepartment(RelationshipType.PersonalTutor, dept1) should be (Seq(m5))
		dao.getStudentsWithoutRelationshipByDepartment(RelationshipType.PersonalTutor, dept2) should be (Seq(m6))
		dao.getStudentsWithoutRelationshipByDepartment(null, dept1) should be (Seq())
	}
}
