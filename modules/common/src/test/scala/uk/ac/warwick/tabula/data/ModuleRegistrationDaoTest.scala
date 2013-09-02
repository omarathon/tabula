package uk.ac.warwick.tabula.data

import org.junit.Before
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.data.model.ModuleRegistration
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.ModuleSelectionStatus

class ModuleRegistrationDaoTest extends PersistenceTestBase {

	val modRegDao = new ModuleRegistrationDaoImpl
	val moduleDao = new ModuleDaoImpl

	@Before
	def setup() {
		modRegDao.sessionFactory = sessionFactory
		moduleDao.sessionFactory = sessionFactory
	}


/*	@Test def testGetByStudentCourseDetailsAndYear {
		transactional { tx =>
			val scd: StudentCourseDetails =
			val nonexistantModReg = modRegDao.getByStudentCourseDetailsAndYear("0070790/1", AcademicYear(2012))
			nonexistantModReg should be (Seq())

			val module = new Module
			module.code = "ab123"
			moduleDao.saveOrUpdate(module)

			val modReg = new ModuleRegistration("0123456/1", "ab123", 10.0, AcademicYear(2012))
			modReg.assessmentGroup = "D"
			modReg.selectionStatus = ModuleSelectionStatus.OptionalCore
			modRegDao.saveOrUpdate(modReg)

			val retrievedModReg = modRegDao.getBySprCodeAndYear("0123456/1", AcademicYear(2012)).head

			retrievedModReg.isInstanceOf[ModuleRegistration] should be (true)
			retrievedModReg.sprCode should be ("0123456/1")
			retrievedModReg.moduleCode should be ("ab123")
			retrievedModReg.cats should be (10.0)
			retrievedModReg.academicYear should be (AcademicYear(2012))
			retrievedModReg.assessmentGroup should be ("D")
			retrievedModReg.selectionStatus should be (ModuleSelectionStatus.OptionalCore)

			// now try adding a second module for the same person
			val module2 = new Module
			module2.code = "cd456"
			moduleDao.saveOrUpdate(module2)

			val modReg2 = new ModuleRegistration("0123456/1", "cd456", 30.0, AcademicYear(2012))
			modReg2.assessmentGroup = "E"
			modReg2.selectionStatus = ModuleSelectionStatus.Core
			modRegDao.saveOrUpdate(modReg2)

			val retrievedModRegSet = modRegDao.getBySprCodeAndYear("0123456/1", AcademicYear(2012))
			retrievedModRegSet.size should be (2)

			val abModule = retrievedModRegSet.find( _.moduleCode == "ab123")
			abModule.size should be (1)
			abModule.get.cats should be (10.0)
			abModule.get.assessmentGroup should be ("D")
			abModule.get.selectionStatus should be (ModuleSelectionStatus.OptionalCore)

			val cdModule = retrievedModRegSet.find( _.moduleCode == "cd456")
			cdModule.size should be (1)
			cdModule.get.cats should be (30.0)
			cdModule.get.assessmentGroup should be ("E")
			cdModule.get.selectionStatus should be (ModuleSelectionStatus.Core)
		}
	}*/

/*	@Test def testGetByNotionalKey {
		transactional { tx =>
			val nonexistantModReg = modRegDao.getByNotionalKey("0123456/1", "ab123", 10.0, AcademicYear(2012))
			nonexistantModReg should be (None)

			val module = new Module
			module.code = "ab123"
			moduleDao.saveOrUpdate(module)

			val modReg = new ModuleRegistration("0123456/1", "ab123", 10.0, AcademicYear(2012))
			modReg.assessmentGroup = "D"
			modReg.selectionStatus = ModuleSelectionStatus.OptionalCore
			modRegDao.saveOrUpdate(modReg)

			val retrievedModReg = modRegDao.getByNotionalKey("0123456/1", "ab123", 10.0, AcademicYear(2012)).get

			retrievedModReg.isInstanceOf[ModuleRegistration] should be (true)
			retrievedModReg.sprCode should be ("0123456/1")
			retrievedModReg.moduleCode should be ("ab123")
			retrievedModReg.cats should be (10.0)
			retrievedModReg.academicYear should be (AcademicYear(2012))
			retrievedModReg.assessmentGroup should be ("D")
			retrievedModReg.selectionStatus should be (ModuleSelectionStatus.OptionalCore)
		}
	}*/

	@Test def testGetByUsercodeAndYear {
		transactional { tx =>
			val nonexistantModReg = modRegDao.getByUsercodeAndYear("cusdx", AcademicYear(2012))
			nonexistantModReg should be (None)

/*			val module = new Module
			module.code = "ab123"
			moduleDao.saveOrUpdate(module)

			val modReg = new ModuleRegistration("0123456/1", "ab123", 10.0, AcademicYear(2012))
			modReg.assessmentGroup = "D"
			modReg.selectionStatus = ModuleSelectionStatus.OptionalCore
			modRegDao.saveOrUpdate(modReg)

			val retrievedModReg = modRegDao.getByNotionalKey("0123456/1", "ab123", 10.0, AcademicYear(2012)).get

			retrievedModReg.isInstanceOf[ModuleRegistration] should be (true)
			retrievedModReg.sprCode should be ("0123456/1")
			retrievedModReg.moduleCode should be ("ab123")
			retrievedModReg.cats should be (10.0)
			retrievedModReg.academicYear should be (AcademicYear(2012))
			retrievedModReg.assessmentGroup should be ("D")
			retrievedModReg.selectionStatus should be (ModuleSelectionStatus.OptionalCore)*/
		}
	}

}
