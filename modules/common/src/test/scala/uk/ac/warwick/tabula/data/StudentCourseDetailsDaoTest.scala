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
	val dao = new StudentCourseDetailsDaoImpl

	@Before def setup() {
		memberDao.sessionFactory = sessionFactory
		dao.sessionFactory = sessionFactory
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

		dao.getByScjCode("1000001/1").get.department should be (dept1)
		dao.getByScjCode("1000001/1").get.student.universityId should be ("1000001")
		dao.getStudentBySprCode("1000001/1").get.universityId should be ("1000001")

		session.delete(dept1)
		session.delete(dept2)
		session.delete(stu1)
		session.delete(stu2)

		session.flush()
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

		dao.findByDepartment(dept1).head.student should be(stu1)
		dao.findByDepartment(dept2).head.student should be(stu2)

	}

}
