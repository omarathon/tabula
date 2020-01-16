package uk.ac.warwick.tabula.data

import org.junit.Before
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, PersistenceTestBase}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.ModuleRegistration
import uk.ac.warwick.tabula.data.model.ModuleSelectionStatus
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.JavaImports.JBigDecimal

class ModuleRegistrationDaoTest extends PersistenceTestBase {
  val memDao = new AutowiringMemberDaoImpl
  val modRegDao = new ModuleRegistrationDaoImpl
  val moduleDao = new ModuleDaoImpl
  val scdDao = new StudentCourseDetailsDaoImpl

  @Before
  def setup(): Unit = {
    memDao.sessionFactory = sessionFactory
    modRegDao.sessionFactory = sessionFactory
    moduleDao.sessionFactory = sessionFactory
    scdDao.sessionFactory = sessionFactory
  }

  @Test def testModReg: Unit = {
    transactional { tx =>
      val stuMem = Fixtures.student("0123456", "abcde")
      memDao.saveOrUpdate(stuMem)

      val scd = stuMem.mostSignificantCourse

      val year = AcademicYear(2012)
      val nonexistantModReg = modRegDao.getByUsercodesAndYear(Seq("abcde"), year)
      nonexistantModReg should be(Seq())

      val module = new Module
      module.code = "ab123"
      moduleDao.saveOrUpdate(module)

      val modReg = new ModuleRegistration(scd.scjCode, module, new JBigDecimal("10.0"), AcademicYear(2012), "A")
      modReg.assessmentGroup = "D"
      modReg.selectionStatus = ModuleSelectionStatus.OptionalCore
      modRegDao.saveOrUpdate(modReg)

      val retrievedModReg = modRegDao.getByUsercodesAndYear(Seq("abcde"), AcademicYear(2012)).head

      retrievedModReg.isInstanceOf[ModuleRegistration] should be(true)
      retrievedModReg._scjCode should be ("0123456/1")
      retrievedModReg.module.code should be("ab123")
      retrievedModReg.cats should be(new JBigDecimal("10.0"))
      retrievedModReg.academicYear should be(AcademicYear(2012))
      retrievedModReg.assessmentGroup should be("D")
      retrievedModReg.selectionStatus should be(ModuleSelectionStatus.OptionalCore)

      // now try adding a second module for the same person
      val module2 = new Module
      module2.code = "cd456"
      moduleDao.saveOrUpdate(module2)

      val modReg2 = new ModuleRegistration(scd.scjCode, module2, new JBigDecimal("30.0"), AcademicYear(2012), "A")
      modReg2.assessmentGroup = "E"
      modReg2.selectionStatus = ModuleSelectionStatus.Core
      modRegDao.saveOrUpdate(modReg2)

      val retrievedModRegSet = modRegDao.getByUsercodesAndYear(Seq("abcde"), AcademicYear(2012))
      retrievedModRegSet.size should be(2)

      val abModule = retrievedModRegSet.find(_.module.code == "ab123")
      abModule.size should be(1)
      abModule.get.cats should be(new JBigDecimal("10.0"))
      abModule.get.assessmentGroup should be("D")
      abModule.get.selectionStatus should be(ModuleSelectionStatus.OptionalCore)

      val cdModule = retrievedModRegSet.find(_.module.code == "cd456")
      cdModule.size should be(1)
      cdModule.get.cats should be(new JBigDecimal("30.0"))
      cdModule.get.assessmentGroup should be("E")
      cdModule.get.selectionStatus should be(ModuleSelectionStatus.Core)
    }
  }

  @Test def testGetByNotionalKey: Unit = {
    transactional { tx =>
      val stuMem = Fixtures.student("0123456", "abcde")
      memDao.saveOrUpdate(stuMem)

      val scd = stuMem.mostSignificantCourse

      val module = new Module

      val nonexistantModReg = modRegDao.getByNotionalKey(scd, module, new JBigDecimal("10.0"), AcademicYear(2012), "A")
      nonexistantModReg should be(None)

      module.code = "ab123"
      moduleDao.saveOrUpdate(module)

      val modReg = new ModuleRegistration(scd.scjCode, module, new JBigDecimal("10.0"), AcademicYear(2012), "A")
      modReg.assessmentGroup = "D"
      modReg.selectionStatus = ModuleSelectionStatus.OptionalCore
      modRegDao.saveOrUpdate(modReg)

      scd.addModuleRegistration(modReg)

      scdDao.saveOrUpdate(scd)

      val retrievedModReg = modRegDao.getByNotionalKey(scd, module, new JBigDecimal("10.0"), AcademicYear(2012), "A").get

      retrievedModReg.isInstanceOf[ModuleRegistration] should be(true)
      retrievedModReg._scjCode should be("0123456/1")
      retrievedModReg.module.code should be("ab123")
      retrievedModReg.cats should be(new JBigDecimal("10.0"))
      retrievedModReg.academicYear should be(AcademicYear(2012))
      retrievedModReg.assessmentGroup should be("D")
      retrievedModReg.selectionStatus should be(ModuleSelectionStatus.OptionalCore)
    }
  }
}
