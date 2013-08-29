package uk.ac.warwick.tabula.data

import org.junit.Before

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.data.model.ModuleRegistration
import uk.ac.warwick.tabula.data.model.ModuleRegistration

class ModuleRegistrationDaoTest extends PersistenceTestBase {

	val dao = new ModuleRegistrationDaoImpl

	@Before
	def setup() {
		dao.sessionFactory = sessionFactory
	}


	@Test def testGetBySprcode {
		transactional { tx =>
			val sprCode = dao.getBySprCode("0070790/1")
			sprCode should be (Seq())
		}
	}

	@Test def testGetByNotionalKey {
		transactional { tx =>
			var cats = 30.0 // Double by default
			var modReg = dao.getByNotionalKey("0070790/1", "gs101", cats, AcademicYear(2012))
			modReg should be (None)
		}
	}

	@Test def testSaveOrUpdate {
		transactional { tx =>
			val modReg = new ModuleRegistration("0123456/1", "ab123", AcademicYear(2012), 10.0)
			modReg.assessmentGroup = "D"
			modReg.selectionStatusCode = "CO"
			dao.saveOrUpdate(modReg)
			val retrievedModReg = dao.getByNotionalKey("0123456/1", "ab123", 10.0, AcademicYear(2012)).get

			retrievedModReg.isInstanceOf[ModuleRegistration] should be (true)
			retrievedModReg.sprCode should be ("0123456/1")
			retrievedModReg.moduleCode should be ("ab123")
			retrievedModReg.cats should be (10.0)
			retrievedModReg.academicYear should be (AcademicYear(2012))
			retrievedModReg.assessmentGroup should be ("D")
			retrievedModReg.selectionStatusCode should be ("CO")
		}
	}

}
