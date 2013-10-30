package uk.ac.warwick.tabula.scheduling.commands.imports

import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.{AppContextTestBase, Fixtures}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.{FileDao, MemberDao}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.DegreeType.Postgraduate
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.services.SupervisorImporter
import uk.ac.warwick.tabula.scheduling.services.ModuleRegistrationRow
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.LocalDate
import org.joda.time.DateTime
import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.data.StudentCourseDetailsDao
import uk.ac.warwick.tabula.data.ModuleRegistrationDao

class ImportModuleRegistrationsCommandTest extends PersistenceTestBase with Mockito with Logging {

	trait Environment {
		val stu = Fixtures.student(universityId = "0000001", userId="student")
		session.saveOrUpdate(stu)

		val scd = stu.mostSignificantCourseDetails.get
		session.saveOrUpdate(scd)

		val mod = Fixtures.module("ax101", "Pointless Deliberations")
		session.saveOrUpdate(mod)
		session.flush

		val mr = new ModuleRegistration(scd, mod, new java.math.BigDecimal(30), new AcademicYear(2013), "A")
		session.saveOrUpdate(mr)
		session.flush

		val cats = new java.math.BigDecimal(30)
		val year = new AcademicYear(2013)
		val occurrence = "O"

		val madService = smartMock[ModuleAndDepartmentService]
		madService.getModuleBySitsCode("AX101-30") returns Some(mod)

		val modRegRow1 = new ModuleRegistrationRow(scd.scjCode, "AX101-30", cats, "A", "C", occurrence, year)
		modRegRow1.madService = madService

		val modRegRow2 = new ModuleRegistrationRow(scd.scjCode, "AX101-30", cats, "A", "O", occurrence, year)
		modRegRow2.madService = madService

		val scdDao = smartMock[StudentCourseDetailsDao]
		scdDao.getByScjCode("0000001/1") returns Some(scd)

		val mrDao = smartMock[ModuleRegistrationDao]
		mrDao.getByNotionalKey(scd, mod, cats, year, occurrence) returns Some(mr)
	}

	@Transactional
	@Test def testCaptureModuleRegistration() {
		new Environment {

			// apply the command
			val command = new ImportModuleRegistrationsCommand(modRegRow1)
			command.moduleAndDepartmentService = madService
			command.studentCourseDetailsDao = scdDao
			command.moduleRegistrationDao = mrDao

			val newModReg = command.applyInternal.get

			// check results
			newModReg.academicYear should be (new AcademicYear(2013))
			newModReg.assessmentGroup should be ("A")
			newModReg.module should be (mod)
			newModReg.cats should be (cats)
			newModReg.occurrence should be (occurrence)
			newModReg.selectionStatus.description should be ("Core")
			newModReg.studentCourseDetails should be (scd)
			newModReg.lastUpdatedDate.getDayOfMonth should be (LocalDate.now.getDayOfMonth)

			// now reset the last updated date to 10 days ago:
			val tenDaysAgo = DateTime.now.minusDays(10)
			newModReg.lastUpdatedDate = tenDaysAgo
			newModReg.lastUpdatedDate.getDayOfMonth should be (tenDaysAgo.getDayOfMonth)
			session.flush

			// now re-import the same mod reg - the lastupdateddate shouldn't change
			val command2 = new ImportModuleRegistrationsCommand(modRegRow1)
			command2.moduleAndDepartmentService = madService
			command2.studentCourseDetailsDao = scdDao
			command2.moduleRegistrationDao = mrDao


			val newModReg2 = command2.applyInternal.get
			newModReg2.lastUpdatedDate.getDayOfMonth should be (tenDaysAgo.getDayOfMonth)

			// try just changing the selection status:
			val command3 = new ImportModuleRegistrationsCommand(modRegRow2)
			command3.moduleAndDepartmentService = madService
			command3.studentCourseDetailsDao = scdDao
			command3.moduleRegistrationDao = mrDao

			val newModReg3 = command3.applyInternal.get
			newModReg3.selectionStatus.description should be ("Option")
		}
	}
}
