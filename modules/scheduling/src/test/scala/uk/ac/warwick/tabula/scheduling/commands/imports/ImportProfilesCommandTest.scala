package uk.ac.warwick.tabula.scheduling.commands.imports

import java.sql.ResultSet
import scala.collection.JavaConverters.{asScalaBufferConverter, _}
import scala.language.implicitConversions
import org.joda.time.DateTime
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, PersistenceTestBase}
import uk.ac.warwick.tabula.data.{MemberDaoImpl, ModuleRegistrationDaoImpl, StudentCourseDetailsDaoImpl, StudentCourseYearDetailsDaoImpl}
import uk.ac.warwick.tabula.data.model.{ModuleRegistration, StudentCourseDetails}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportStudentRowCommand
import uk.ac.warwick.tabula.scheduling.helpers.ImportRowTracker
import uk.ac.warwick.tabula.scheduling.services.{MembershipInformation, MembershipMember, ProfileImporter, SitsAcademicYearAware, SitsAcademicYearService}
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, ProfileService, SmallGroupService, UserLookupService}
import uk.ac.warwick.userlookup.User
import java.sql.ResultSetMetaData

class ImportProfilesCommandTest extends PersistenceTestBase with Mockito with Logging with SitsAcademicYearAware {
	trait Environment {
		val year = new AcademicYear(2013)

		// set up a department
		val dept = Fixtures.department("EN", "English")

		// set up a student
		val stu = Fixtures.student(universityId = "0000001", userId="student")
		session.saveOrUpdate(stu)

		val scd = stu.mostSignificantCourseDetails.get
		session.saveOrUpdate(scd)
		session.flush

		val scyd = scd.studentCourseYearDetails.asScala.head

		// create a module
		val existingMod = Fixtures.module("ax101", "Pointless Deliberations")
		session.saveOrUpdate(existingMod)
		session.flush

		// register the student on the module
		val existingMr = new ModuleRegistration(scd, existingMod, new java.math.BigDecimal(30), new AcademicYear(2013), "A")
		session.saveOrUpdate(existingMr)
		scd.moduleRegistrations.add(existingMr)
		session.saveOrUpdate(scd)
		session.flush

		// make another module
		val newMod = Fixtures.module("zy909", "Meaningful Exchanges")
		session.saveOrUpdate(newMod)
		session.flush

		// mock required services
		val mrDao = smartMock[ModuleRegistrationDaoImpl]
		mrDao.sessionFactory = sessionFactory
		mrDao.getByUsercodesAndYear(Seq("abcde"), year) returns Seq(existingMr)

		val memberDao = smartMock[MemberDaoImpl]
		memberDao.sessionFactory = sessionFactory

		val scdDao = smartMock[StudentCourseDetailsDaoImpl]
		scdDao.sessionFactory = sessionFactory

		val scydDao = smartMock[StudentCourseYearDetailsDaoImpl]
		scydDao.sessionFactory = sessionFactory

		val sitsAcademicYearService = smartMock[SitsAcademicYearService]
		sitsAcademicYearService.getCurrentSitsAcademicYearString returns "13/14"

		val smallGroupService = smartMock[SmallGroupService]

		val command = new ImportProfilesCommand
		command.sessionFactory = sessionFactory
		command.sitsAcademicYearService = sitsAcademicYearService
		command.moduleRegistrationDao = mrDao
		command.smallGroupService = smallGroupService
		command.memberDao = memberDao
		command.studentCourseDetailsDao = scdDao
		command.studentCourseYearDetailsDao = scydDao
	}

	@Transactional
	@Test def testDeleteOldModuleRegistrations() {
		new Environment {
			// check that if the new MR matches the old, it will not be deleted:
			command.deleteOldModuleRegistrations(Seq("abcde"), Seq(existingMr))
			scd.moduleRegistrations.contains(existingMr) should be (true)
			session.flush

			val newMr = new ModuleRegistration(scd, newMod, new java.math.BigDecimal(30), new AcademicYear(2013), "A")
			session.saveOrUpdate(newMr)
			session.flush
			scd.moduleRegistrations.add(newMr)
			session.flush

			// now check that if the new MR does not match the old, the old will be deleted:
			command.deleteOldModuleRegistrations(Seq("abcde"), Seq(newMr))
			session.flush
			scd.moduleRegistrations.contains(existingMr) should be (false)
		}
	}

	@Transactional
	@Test def testStampMissingRows() {
		new Environment {
			memberDao.getStudentsPresentInSits returns Seq(stu)
			scdDao.getAllPresentInSits returns Seq(scd)
			scydDao.getAllPresentInSits returns Seq(scyd)

			val tracker = new ImportRowTracker
			tracker.studentsSeen.add(stu)
			tracker.studentCourseDetailsSeen.add(scd)
			tracker.studentCourseYearDetailsSeen.add(scyd)

			command.stampMissingRows(tracker, DateTime.now)

			stu.missingFromImportSince should be (null)
			scd.missingFromImportSince should be (null)
			scyd.missingFromImportSince should be (null)

			tracker.studentsSeen.remove(stu)

			command.stampMissingRows(tracker, DateTime.now)

			stu.missingFromImportSince should not be (null)
			scd.missingFromImportSince should be (null)
			scyd.missingFromImportSince should be (null)

			tracker.studentCourseDetailsSeen.remove(scd)
			command.stampMissingRows(tracker, DateTime.now)

			stu.missingFromImportSince should not be (null)
			scd.missingFromImportSince should not be (null)
			scyd.missingFromImportSince should be (null)

			tracker.studentCourseYearDetailsSeen.remove(scyd)
			command.stampMissingRows(tracker, DateTime.now)

			stu.missingFromImportSince should not be (null)
			scd.missingFromImportSince should not be (null)
			scyd.missingFromImportSince should not be (null)

		}
	}

	@Transactional
	@Test def testUpdateMissingForIndividual() {
		new Environment {
			val tracker = new ImportRowTracker
			tracker.studentsSeen.add(stu)
			tracker.studentCourseDetailsSeen.add(scd)
			tracker.studentCourseYearDetailsSeen.add(scyd)

			command.updateMissingForIndividual(stu, tracker)

			stu.missingFromImportSince should be (null)
			scd.missingFromImportSince should be (null)
			scyd.missingFromImportSince should be (null)

			tracker.studentsSeen.remove(stu)

			command.updateMissingForIndividual(stu, tracker)

			stu.missingFromImportSince should not be (null)
			scd.missingFromImportSince should be (null)
			scyd.missingFromImportSince should be (null)

			tracker.studentCourseDetailsSeen.remove(scd)
			command.updateMissingForIndividual(stu, tracker)

			stu.missingFromImportSince should not be (null)
			scd.missingFromImportSince should not be (null)
			scyd.missingFromImportSince should be (null)

			tracker.studentCourseYearDetailsSeen.remove(scyd)
			command.updateMissingForIndividual(stu, tracker)

			stu.missingFromImportSince should not be (null)
			scd.missingFromImportSince should not be (null)
			scyd.missingFromImportSince should not be (null)

		}
	}
}
