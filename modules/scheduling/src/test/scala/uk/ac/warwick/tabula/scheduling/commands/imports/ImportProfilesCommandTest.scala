package uk.ac.warwick.tabula.scheduling.commands.imports

import org.springframework.transaction.annotation.Transactional

import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, PersistenceTestBase}
import uk.ac.warwick.tabula.data.ModuleRegistrationDaoImpl
import uk.ac.warwick.tabula.data.model.ModuleRegistration
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.services.{SitsAcademicYearAware, SitsAcademicYearService}

class ImportProfilesCommandTest extends PersistenceTestBase with Mockito with Logging with SitsAcademicYearAware {

	trait Environment {
		val stu = Fixtures.student(universityId = "0000001", userId="student")
		session.saveOrUpdate(stu)

		val scd = stu.mostSignificantCourseDetails.get
		session.saveOrUpdate(scd)
		session.flush

		val existingMod = Fixtures.module("ax101", "Pointless Deliberations")
		session.saveOrUpdate(existingMod)
		session.flush

		val existingMr = new ModuleRegistration(scd, existingMod, new java.math.BigDecimal(30), new AcademicYear(2013), "A")
		session.saveOrUpdate(existingMr)
		scd.moduleRegistrations.add(existingMr)
		session.saveOrUpdate(scd)
		session.flush

		val newMod = Fixtures.module("zy909", "Meaningful Exchanges")
		session.saveOrUpdate(existingMod)
		session.flush

		val year = new AcademicYear(2013)

		val mrDao = smartMock[ModuleRegistrationDaoImpl]
		mrDao.getByUsercodesAndYear(Seq("abcde"), year) returns Seq(existingMr)
	}

	@Transactional
	@Test def testDeleteOldModuleRegistrations() {
		new Environment {

			val sitsAcademicYearService = smartMock[SitsAcademicYearService]
			sitsAcademicYearService.getCurrentSitsAcademicYearString returns "13/14"

			val command = new ImportProfilesCommand
			command.sitsAcademicYearService = sitsAcademicYearService
			command.moduleRegistrationDao = mrDao

			// pass in the full, revised set of module registrations
			command.deleteOldModuleRegistrations(Seq("abcde"), Seq(existingMr))
			scd.moduleRegistrations.contains(existingMr) should be (true)

			val newMr = new ModuleRegistration(scd, newMod, new java.math.BigDecimal(30), new AcademicYear(2013), "A")
			session.saveOrUpdate(newMr)
			session.flush
			scd.moduleRegistrations.add(newMr)
			session.flush
			command.deleteOldModuleRegistrations(Seq("abcde"), Seq(newMr))
			scd.moduleRegistrations.contains(existingMr) should be (false)

		}
	}
}
