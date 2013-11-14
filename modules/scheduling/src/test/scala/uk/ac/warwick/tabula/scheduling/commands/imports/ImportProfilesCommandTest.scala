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
import uk.ac.warwick.tabula.data.model.StudentCourseYearKey
import scala.collection.mutable.HashSet

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

		val scyd = scd.freshStudentCourseYearDetails.head

		// set up another student
		val stu2 = Fixtures.student(universityId = "0000002", userId="student2")
		session.saveOrUpdate(stu2)

		val scd2 = stu2.mostSignificantCourseDetails.get
		session.saveOrUpdate(scd2)
		session.flush

		val scyd2 = scd2.freshStudentCourseYearDetails.head

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

		val key1 = new StudentCourseYearKey(scyd.studentCourseDetails.scjCode, scyd.sceSequenceNumber)
		scydDao.getIdFromKey(key1) returns Some("1")

		val key2 = new StudentCourseYearKey(scyd2.studentCourseDetails.scjCode, scyd2.sceSequenceNumber)
		scydDao.getIdFromKey(key2) returns Some("2")

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
			memberDao.getFreshUniversityIds returns Seq(stu.universityId)
			scdDao.getFreshScjCodes returns Seq(scd.scjCode)
			scydDao.getFreshKeys returns Seq(new StudentCourseYearKey(scyd.studentCourseDetails.scjCode, scyd.sceSequenceNumber))

			val tracker = new ImportRowTracker
			tracker.universityIdsSeen.add(stu.universityId)
			tracker.scjCodesSeen.add(scd.scjCode)

			tracker.studentCourseYearDetailsSeen.add(key1)

			command.stampMissingRows(tracker, DateTime.now)
			session.flush

			stu.missingFromImportSince should be (null)
			scd.missingFromImportSince should be (null)
			scyd.missingFromImportSince should be (null)

			tracker.universityIdsSeen.remove(stu.universityId)

			command.stampMissingRows(tracker, DateTime.now)
			session.flush

			scd.missingFromImportSince should be (null)
			scyd.missingFromImportSince should be (null)
			logger.warn("about to see whether stu has been stamped")
			stu.missingFromImportSince should not be (null)

			tracker.scjCodesSeen.remove(scd.scjCode)
			command.stampMissingRows(tracker, DateTime.now)

			stu.missingFromImportSince should not be (null)
			scd.missingFromImportSince should not be (null)
			scyd.missingFromImportSince should be (null)

			tracker.studentCourseYearDetailsSeen.remove(key1)
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
			tracker.universityIdsSeen.add(stu.universityId)
			tracker.scjCodesSeen.add(scd.scjCode)

			val key = new StudentCourseYearKey(scyd.studentCourseDetails.scjCode, scyd.sceSequenceNumber)
			tracker.studentCourseYearDetailsSeen.add(key)

			command.updateMissingForIndividual(stu, tracker)

			stu.missingFromImportSince should be (null)
			scd.missingFromImportSince should be (null)
			scyd.missingFromImportSince should be (null)

			tracker.universityIdsSeen.remove(stu.universityId)

			command.updateMissingForIndividual(stu, tracker)

			stu.missingFromImportSince should not be (null)
			scd.missingFromImportSince should be (null)
			scyd.missingFromImportSince should be (null)

			tracker.scjCodesSeen.remove(scd.scjCode)
			command.updateMissingForIndividual(stu, tracker)

			stu.missingFromImportSince should not be (null)
			scd.missingFromImportSince should not be (null)
			scyd.missingFromImportSince should be (null)

			tracker.studentCourseYearDetailsSeen.remove(key)
			command.updateMissingForIndividual(stu, tracker)

			stu.missingFromImportSince should not be (null)
			scd.missingFromImportSince should not be (null)
			scyd.missingFromImportSince should not be (null)

		}
	}

	@Test
	def testConvertKeysToIds {
		new Environment {

			val scydFromDb = scydDao.getBySceKey(scyd.studentCourseDetails, scyd.sceSequenceNumber)
			val idForScyd = scydFromDb.get.id

			val scyd2FromDb = scydDao.getBySceKey(scyd2.studentCourseDetails, scyd2.sceSequenceNumber)
			val idForScyd2 = scyd2FromDb.get.id

			val keys = new HashSet[StudentCourseYearKey]

			keys.add(key1)
			keys.add(key2)

			val ids = command.convertKeysToIds(keys)

			ids.size should be (2)

			ids.contains(idForScyd) should be (true)
			ids.contains(idForScyd2) should be (true)

		}
	}
}
