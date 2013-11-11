package uk.ac.warwick.tabula.data

import scala.collection.JavaConverters.asScalaBufferConverter

import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import org.junit.After
import org.junit.Before

import uk.ac.warwick.tabula.{Mockito, PersistenceTestBase, Fixtures}
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.Logging

class StudentCourseDetailsDaoTest extends PersistenceTestBase with Logging with Mockito {

	val memberDao = new MemberDaoImpl
	val studentCourseDetailsDao = new StudentCourseDetailsDaoImpl

	@Before def setup() {
		memberDao.sessionFactory = sessionFactory
		studentCourseDetailsDao.sessionFactory = sessionFactory
		transactional { tx =>
			session.enableFilter(Member.ActiveOnlyFilter)
		}
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

		studentCourseDetailsDao.getByScjCode("1000001/1").get.department should be (dept1)
		studentCourseDetailsDao.getByScjCode("1000001/1").get.student.universityId should be ("1000001")
		studentCourseDetailsDao.getStudentBySprCode("1000001/1").get.universityId should be ("1000001")
	}

	@Test def getBySprCode = transactional { tx =>
		val dept1 = Fixtures.department("ms", "Motorsport")
		val dept2 = Fixtures.department("vr", "Vehicle Repair")

		session.save(dept1)
		session.save(dept2)

		session.flush

		val stu1 = Fixtures.student(universityId = "2000001", userId="student", department=dept1, courseDepartment=dept1)

		// the student fixture comes with one free studentCourseDetails - add another and override the defaults SPR code:
		val stu1_scd2 = Fixtures.studentCourseDetails(stu1, dept1, null, "2000001/2")
		stu1_scd2.sprCode = "2000001/2"
		memberDao.saveOrUpdate(stu1)
		studentCourseDetailsDao.saveOrUpdate(stu1_scd2)
		session.flush

		studentCourseDetailsDao.getByScjCode("2000001/1").size should be (1)
		studentCourseDetailsDao.getBySprCode("2000001/1").size should be (1)
		studentCourseDetailsDao.getBySprCode("2000001/2").size should be (1)
		session.flush

		val stu1_scd3 = Fixtures.studentCourseDetails(stu1, dept1, null, "2000001/3")
		stu1_scd3.sprCode = "2000001/2"
		session.flush

		studentCourseDetailsDao.getBySprCode("2000001/2").size should be (2)

		val stu2 = Fixtures.student(universityId = "2000002", userId="student", department=dept2, courseDepartment=dept2)

		memberDao.saveOrUpdate(stu1)
		memberDao.saveOrUpdate(stu2)

		session.flush()

		studentCourseDetailsDao.getBySprCode("2000001/1").head.department should be (dept1)
		studentCourseDetailsDao.getBySprCode("2000001/1").head.student.universityId should be ("2000001")
		studentCourseDetailsDao.getStudentBySprCode("2000001/1").get.universityId should be ("2000001")
	}


	@Test def getByDepartment = transactional { tx =>
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

		studentCourseDetailsDao.findByDepartment(dept1).head.student should be(stu1)
		studentCourseDetailsDao.findByDepartment(dept2).head.student should be(stu2)

	}

	@Test
	def testGetStudentCourseDetailsPresentInSits = transactional { tx =>
		val dept1 = Fixtures.department("hm", "History of Music")
		val dept2 = Fixtures.department("ar", "Architecture")

		session.saveOrUpdate(dept1)
		session.saveOrUpdate(dept2)

		val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1)
		val stu2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2)
		val stu3 = Fixtures.student(universityId = "1000003", userId="student", department=dept2, courseDepartment=dept2)
		val stu4 = Fixtures.student(universityId = "1000004", userId="student", department=dept2, courseDepartment=dept2)

		memberDao.saveOrUpdate(stu1)
		memberDao.saveOrUpdate(stu2)
		memberDao.saveOrUpdate(stu3)
		memberDao.saveOrUpdate(stu4)

		studentCourseDetailsDao.getAllFreshInSits.size should be (4)

		stu2.mostSignificantCourse.missingFromImportSince = DateTime.now
		session.saveOrUpdate(stu2.mostSignificantCourse)

		studentCourseDetailsDao.getAllFreshInSits.size should be (3)
	}

}
