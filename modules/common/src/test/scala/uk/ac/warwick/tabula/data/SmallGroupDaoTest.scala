package uk.ac.warwick.tabula.data

import org.junit.Before

import uk.ac.warwick.tabula.{AcademicYear, Fixtures, PersistenceTestBase}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat

class SmallGroupDaoTest extends PersistenceTestBase {

	val smallGroupDao = new SmallGroupDaoImpl
	val moduleDao = new ModuleDaoImpl
	val module = Fixtures.module("kt123","Kinesthetic Teaching")
	val smallGroupSet = Fixtures.smallGroupSet("Test Small Group Set")

	@Before
	def setup() {
		smallGroupDao.sessionFactory = sessionFactory
		moduleDao.sessionFactory = sessionFactory
	}

	@Test def findByModuleAndYear = transactional { tx =>
		smallGroupSet.academicYear = new AcademicYear(2013)
		smallGroupSet.format = SmallGroupFormat.Seminar

		moduleDao.saveOrUpdate(module)
		session.flush

		smallGroupSet.module = module

		smallGroupDao.saveOrUpdate(smallGroupSet)
		session.flush

		val smallGroup = Fixtures.smallGroup("Test Small Group")
		smallGroup.groupSet = smallGroupSet

		smallGroupSet.groups.add(smallGroup)

		smallGroupDao.saveOrUpdate(smallGroup)
		session.flush

		smallGroupDao.findByModuleAndYear(module, AcademicYear(2013)) should be (Seq(smallGroup))
	}
}
