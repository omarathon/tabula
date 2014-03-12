package uk.ac.warwick.tabula.data

import org.junit.Before

import uk.ac.warwick.tabula.{AcademicYear, Fixtures, PersistenceTestBase}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat

class SmallGroupDaoTest extends PersistenceTestBase {

	val smallGroupDao = new SmallGroupDaoImpl
	val moduleDao = new ModuleDaoImpl
	val module = Fixtures.module("kt123","Kinesthetic Teaching")
	val moduleWithNoGroups = Fixtures.module("ab123", "No Groups Here")
	val smallGroupSet = Fixtures.smallGroupSet("Test Small Group Set")
	val smallGroup = Fixtures.smallGroup("Test Small Group")

	@Before
	def setup() {
		smallGroupDao.sessionFactory = sessionFactory
		moduleDao.sessionFactory = sessionFactory
		smallGroupSet.academicYear = new AcademicYear(2013)
		smallGroupSet.format = SmallGroupFormat.Seminar
		smallGroupSet.module = module

		smallGroup.groupSet = smallGroupSet
		smallGroupSet.groups.add(smallGroup)

	}

	@Test def findByModuleAndYear = transactional { tx =>
		moduleDao.saveOrUpdate(module)
		session.flush
		smallGroupDao.saveOrUpdate(smallGroupSet)
		session.flush
		smallGroupDao.saveOrUpdate(smallGroup)
		session.flush

		smallGroupDao.findByModuleAndYear(module, AcademicYear(2013)) should be (Seq(smallGroup))
	}

	@Test def hasSmallGroups = transactional { tx =>
		moduleDao.saveOrUpdate(module)
		moduleDao.saveOrUpdate(moduleWithNoGroups)
		session.flush
		smallGroupDao.saveOrUpdate(smallGroupSet)
		session.flush
		smallGroupDao.saveOrUpdate(smallGroup)
		session.flush

		smallGroupDao.hasSmallGroups(module) should be(true)
		smallGroupDao.hasSmallGroups((moduleWithNoGroups)) should be(false)
	}

}
