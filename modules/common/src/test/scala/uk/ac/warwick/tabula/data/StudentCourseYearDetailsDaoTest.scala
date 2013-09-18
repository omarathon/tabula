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

			val scyd = new StudentCourseYearDetails(scd, 1)
			scyd.academicYear = AcademicYear(2013)

			scd.studentCourseYearDetails.add(scyd)

			scydDao.saveOrUpdate(scyd)
			scdDao.saveOrUpdate(scd)

			scd.studentCourseYearDetails.size should be (1)

			val retrievedScyd = scydDao.getBySceKey(scd, 1).get
			retrievedScyd.isInstanceOf[StudentCourseYearDetails] should be (true)
			retrievedScyd.studentCourseDetails.scjCode should be ("0123456/1")
			retrievedScyd.studentCourseDetails.sprCode should be ("0123456/2")
			retrievedScyd.academicYear should be (AcademicYear(2013))
		}
	}
}
