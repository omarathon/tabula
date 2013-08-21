package uk.ac.warwick.tabula.data

import collection.JavaConverters._
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import org.junit.Before
import org.junit.After
import uk.ac.warwick.tabula.{Mockito, PersistenceTestBase, Fixtures}
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.data.model.RelationshipType
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.SitsStatus

// scalastyle:off magic.number
class MemberDaoTest extends PersistenceTestBase with Logging with Mockito {

	val memberDao = new MemberDaoImpl
	val sitsStatusDao = new SitsStatusDaoImpl
	val sprFullyEnrolledStatus = Fixtures.sitsStatus("F", "Fully Enrolled", "Fully Enrolled for this Session")

	@Before def setup() {
		memberDao.sessionFactory = sessionFactory
		sitsStatusDao.sessionFactory = sessionFactory

		transactional { tx =>
			session.enableFilter(Member.ActiveOnlyFilter)
		}
	}

	@After def tidyUp: Unit = transactional { tx =>
		session.disableFilter(Member.ActiveOnlyFilter)

		session.createCriteria(classOf[Member]).list().asInstanceOf[JList[Member]].asScala map { session.delete(_) }
	}

	@Test
	def crud = {
		transactional { tx =>

			sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

			val m1 = Fixtures.student(universityId = "0000001", userId="student", sprStatus=sprFullyEnrolledStatus)
			val m2 = Fixtures.student(universityId = "0000002", userId="student", sprStatus=sprFullyEnrolledStatus)

			val m3 = Fixtures.staff(universityId = "0000003", userId="staff1")
			val m4 = Fixtures.staff(universityId = "0000004", userId="staff2")

			memberDao.saveOrUpdate(m1)
			memberDao.saveOrUpdate(m2)
			memberDao.saveOrUpdate(m3)
			memberDao.saveOrUpdate(m4)

			memberDao.getByUniversityId("0000001") should be (Some(m1))
			memberDao.getByUniversityId("0000002") should be (Some(m2))
			memberDao.getByUniversityId("0000003") should be (Some(m3))
			memberDao.getByUniversityId("0000004") should be (Some(m4))
			memberDao.getByUniversityId("0000005") should be (None)

			session.enableFilter(Member.StudentsOnlyFilter)

			memberDao.getByUniversityId("0000003") should be (None)
			memberDao.getByUniversityId("0000004") should be (None)

			memberDao.getAllByUserId("student", false) should be (Seq(m1, m2))
			memberDao.getByUserId("student", false) should be (Some(m1))
			memberDao.getAllByUserId("student", true) should be (Seq(m1, m2))
			memberDao.getAllByUserId("staff1", false) should be (Seq())
			memberDao.getByUserId("staff1", false) should be (None)
			memberDao.getAllByUserId("staff1", true) should be (Seq(m3))
			memberDao.getByUserId("staff1", true) should be (Some(m3))
			memberDao.getAllByUserId("unknown", false) should be (Seq())
			memberDao.getAllByUserId("unknown", true) should be (Seq())

			session.disableFilter(Member.StudentsOnlyFilter)

			memberDao.getAllByUserId("staff1", false) should be (Seq(m3))
			memberDao.getByUserId("staff1", false) should be (Some(m3))
	}
}

	@Test
  def listUpdatedSince = transactional { tx =>
		val dept1 = Fixtures.department("hi", "History")
		val dept2 = Fixtures.department("fr", "French")

		session.save(dept1)
		session.save(dept2)

		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

		val m1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, sprStatus=sprFullyEnrolledStatus)
		m1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val m2 = Fixtures.student(universityId = "1000002", userId="student", department=dept1, sprStatus=sprFullyEnrolledStatus)
		m2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val m3 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		m3.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val m4 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		m4.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		memberDao.saveOrUpdate(m1)
		memberDao.saveOrUpdate(m2)
		memberDao.saveOrUpdate(m3)
		memberDao.saveOrUpdate(m4)

		memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), 5) should be (Seq(m1, m2, m3, m4))
		memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), 1) should be (Seq(m1))
		memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 0, 0, 0, 0), 5) should be (Seq(m2, m3, m4))
		memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 5, 0, 0, 0, 0), 5) should be (Seq())

		memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), dept1, 5) should be (Seq(m1, m2, m3))
		memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), dept1, 1) should be (Seq(m1))
		memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 0, 0, 0, 0), dept1, 5) should be (Seq(m2, m3))
		memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 5, 0, 0, 0, 0), dept1, 5) should be (Seq())
		memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), dept2, 5) should be (Seq(m4))
	}

	@Test
	def getRegisteredModules: Unit = transactional { tx =>
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

		memberDao.getRegisteredModules("0000001") should be (Seq(mod1))
		memberDao.getRegisteredModules("0000002").toSet should be (Seq(mod1, mod2).toSet)
		memberDao.getRegisteredModules("0000003") should be (Seq(mod2))
		memberDao.getRegisteredModules("0000004") should be (Seq())
	}

	@Test
	def studentRelationshipsCurrentAndByTarget = transactional { tx =>
		val dept1 = Fixtures.department("sp", "Spanish")
		val dept2 = Fixtures.department("en", "English")

		session.save(dept1)
		session.save(dept2)

		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

	  val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		stu1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val stu2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		stu2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val staff1 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		staff1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val staff2 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		staff2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		memberDao.saveOrUpdate(stu1)
		memberDao.saveOrUpdate(stu2)
		memberDao.saveOrUpdate(staff1)
		memberDao.saveOrUpdate(staff2)

		val relBetweenStaff1AndStu1 = StudentRelationship("1000003", RelationshipType.PersonalTutor, "1000001/1")
		val relBetweenStaff1AndStu2 = StudentRelationship("1000003", RelationshipType.PersonalTutor, "1000002/1")

		memberDao.saveOrUpdate(relBetweenStaff1AndStu1)
		memberDao.saveOrUpdate(relBetweenStaff1AndStu2)

		memberDao.getCurrentRelationships(RelationshipType.PersonalTutor, "1000001/1") should be (Seq(relBetweenStaff1AndStu1))
		memberDao.getCurrentRelationships(RelationshipType.PersonalTutor, "1000002/1") should be (Seq(relBetweenStaff1AndStu2))
		memberDao.getCurrentRelationships(RelationshipType.PersonalTutor, "1000003/1") should be (Nil)
		memberDao.getCurrentRelationships(null, "1000001/1") should be (Nil)

		memberDao.getRelationshipsByTarget(RelationshipType.PersonalTutor, "1000001/1") should be (Seq(relBetweenStaff1AndStu1))
		memberDao.getRelationshipsByTarget(RelationshipType.PersonalTutor, "1000002/1") should be (Seq(relBetweenStaff1AndStu2))
		memberDao.getRelationshipsByTarget(RelationshipType.PersonalTutor, "1000003/1") should be (Seq())
		memberDao.getRelationshipsByTarget(null, "1000001/1") should be (Seq())
	}

	@Test
	def studentRelationshipsByDepartmentAndAgent = transactional { tx =>
		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

		val dept1 = Fixtures.department("hm", "History of Music")
		val dept2 = Fixtures.department("ar", "Architecture")

		session.save(dept1)
		session.save(dept2)

		val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		stu1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val stu2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2, sprStatus=sprFullyEnrolledStatus)
		stu2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val staff1 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		staff1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val staff2 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		staff2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		memberDao.saveOrUpdate(stu1)
		memberDao.saveOrUpdate(stu2)
		memberDao.saveOrUpdate(staff1)
		memberDao.saveOrUpdate(staff2)

		val relBetweenStaff1AndStu1 = StudentRelationship("1000003", RelationshipType.PersonalTutor, "1000001/1")
		val relBetweenStaff1AndStu2 = StudentRelationship("1000003", RelationshipType.PersonalTutor, "1000002/1")

		memberDao.saveOrUpdate(relBetweenStaff1AndStu1)
		memberDao.saveOrUpdate(relBetweenStaff1AndStu2)

		// relationship Wires in a ProfileService, awkward.
		// Fortunately we have a chance to inject a mock in here.
		val profileService = smartMock[ProfileService]
		profileService.getStudentBySprCode("1000001/1") returns (Some(stu1))

		val ret = memberDao.getRelationshipsByDepartment(RelationshipType.PersonalTutor, dept1)
		ret(0).profileService = profileService
		ret(0).studentMember.get.universityId should be ("1000001")
		ret(0).studentMember.get.mostSignificantCourseDetails.get.department.code should be ("hm")

		memberDao.getRelationshipsByDepartment(RelationshipType.PersonalTutor, dept1) should be (Seq(relBetweenStaff1AndStu1))
		memberDao.getRelationshipsByDepartment(RelationshipType.PersonalTutor, dept2) should be (Seq(relBetweenStaff1AndStu2))
		memberDao.getRelationshipsByDepartment(null, dept1) should be (Seq())

		memberDao.getRelationshipsByAgent(RelationshipType.PersonalTutor, "1000003").toSet should be (Seq(relBetweenStaff1AndStu1, relBetweenStaff1AndStu2).toSet)
		memberDao.getRelationshipsByAgent(RelationshipType.PersonalTutor, "1000004") should be (Seq())
		memberDao.getRelationshipsByAgent(null, "1000003") should be (Seq())
	}

	@Test
	def studentsWithoutRelationships = transactional { tx =>
		val dept1 = Fixtures.department("af", "Art of Foraging")
		val dept2 = Fixtures.department("tm", "Traditional Music")

		session.save(dept1)
		session.save(dept2)

		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

		val relBetweenStaff1AndStu1 = StudentRelationship("1000003", RelationshipType.PersonalTutor, "1000001/1")
		val relBetweenStaff1AndStu2 = StudentRelationship("1000003", RelationshipType.PersonalTutor, "1000002/1")

		memberDao.saveOrUpdate(relBetweenStaff1AndStu1)
		memberDao.saveOrUpdate(relBetweenStaff1AndStu2)

		val m5 = Fixtures.student(universityId = "1000005", userId="student", department=dept1, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		val m6 = Fixtures.student(universityId = "1000006", userId="student", department=dept2, courseDepartment=dept2, sprStatus=sprFullyEnrolledStatus)

		memberDao.saveOrUpdate(m5)
		memberDao.saveOrUpdate(m6)

		memberDao.getStudentsWithoutRelationshipByDepartment(RelationshipType.PersonalTutor, dept1) should be (Seq(m5))
		memberDao.getStudentsWithoutRelationshipByDepartment(RelationshipType.PersonalTutor, dept2) should be (Seq(m6))
		memberDao.getStudentsWithoutRelationshipByDepartment(null, dept1) should be (Seq())
	}

	@Test def studentsCounting = transactional { tx =>
		val dept1 = Fixtures.department("ms", "Motorsport")
		val dept2 = Fixtures.department("vr", "Vehicle Repair")

		session.save(dept1)
		session.save(dept2)

		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

		val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		stu1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val stu2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2, sprStatus=sprFullyEnrolledStatus)
		stu2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val staff1 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		staff1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val staff2 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		staff2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		memberDao.saveOrUpdate(stu1)
		memberDao.saveOrUpdate(stu2)
		memberDao.saveOrUpdate(staff1)
		memberDao.saveOrUpdate(staff2)

		val relBetweenStaff1AndStu1 = StudentRelationship("1000003", RelationshipType.PersonalTutor, "1000001/1")
		val relBetweenStaff1AndStu2 = StudentRelationship("1000003", RelationshipType.PersonalTutor, "1000002/1")

		memberDao.saveOrUpdate(relBetweenStaff1AndStu1)
		memberDao.saveOrUpdate(relBetweenStaff1AndStu2)

		memberDao.countStudentsByDepartment(dept1) should be (1)
		memberDao.countStudentsByRelationshipAndDepartment(RelationshipType.PersonalTutor, dept1) should be (1)
	}

}
