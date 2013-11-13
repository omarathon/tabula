package uk.ac.warwick.tabula.data

import org.hibernate.annotations.AccessType
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import javax.persistence.Entity
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.ModuleRegistration
import uk.ac.warwick.tabula.data.model.ModuleSelectionStatus
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails
import uk.ac.warwick.tabula.Fixtures
import scala.collection.JavaConverters._
import org.joda.time.DateTime

class StudentCourseYearDetailsDaoTest extends PersistenceTestBase {

	val scydDao = new StudentCourseYearDetailsDaoImpl
	val scdDao = new StudentCourseDetailsDaoImpl
	val memDao = new MemberDaoImpl

	@Before
	def setup() {
		scydDao.sessionFactory = sessionFactory
		scdDao.sessionFactory = sessionFactory
		memDao.sessionFactory = sessionFactory
	}

	@Test def testGetBySceKey {
		transactional { tx =>
			val stuMem = new StudentMember("0123456")
			stuMem.userId = "abcde"
			memDao.saveOrUpdate(stuMem)
			val scd = new StudentCourseDetails(stuMem, "0123456/1")
			scd.sprCode = "0123456/2"
			scdDao.saveOrUpdate(scd)

			val nonexistantYearDetails = scydDao.getBySceKey(scd, 1)
			nonexistantYearDetails should be (None)

			val scyd = new StudentCourseYearDetails(scd, 1,AcademicYear(2013))

			scd.addStudentCourseYearDetails(scyd)

			scydDao.saveOrUpdate(scyd)
			scdDao.saveOrUpdate(scd)

			scd.freshStudentCourseYearDetails.size should be (1)

			val retrievedScyd = scydDao.getBySceKey(scd, 1).get
			retrievedScyd.isInstanceOf[StudentCourseYearDetails] should be (true)
			retrievedScyd.studentCourseDetails.scjCode should be ("0123456/1")
			retrievedScyd.studentCourseDetails.sprCode should be ("0123456/2")
			retrievedScyd.academicYear should be (AcademicYear(2013))
		}
	}

	@Test def testGetAllFresh {
		transactional { tx =>
			val dept1 = Fixtures.department("hm", "History of Music")
			val dept2 = Fixtures.department("ar", "Architecture")

			session.saveOrUpdate(dept1)
			session.saveOrUpdate(dept2)

			val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1)
			val stu2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2)
			val stu3 = Fixtures.student(universityId = "1000003", userId="student", department=dept2, courseDepartment=dept2)
			val stu4 = Fixtures.student(universityId = "1000004", userId="student", department=dept2, courseDepartment=dept2)

			session.saveOrUpdate(stu1)
			session.saveOrUpdate(stu2)
			session.saveOrUpdate(stu3)
			session.saveOrUpdate(stu4)

			scydDao.getFreshKeys.size should be (4)

			val scyd = stu2.mostSignificantCourse.freshStudentCourseYearDetails.head

			scyd.missingFromImportSince = DateTime.now
			session.saveOrUpdate(scyd)

			scydDao.getFreshKeys.size should be (3)
		}
	}
}
