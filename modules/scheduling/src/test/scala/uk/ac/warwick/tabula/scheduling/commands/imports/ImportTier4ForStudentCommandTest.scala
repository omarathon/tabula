package uk.ac.warwick.tabula.scheduling.commands.imports

import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.model.DegreeType.Postgraduate
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.scheduling.services.{Tier4VisaImporter, SupervisorImporter, CasUsageImporter}
import uk.ac.warwick.tabula.helpers.Logging
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.StudentCourseYearDetailsDao

class ImportTier4ForStudentCommandTest extends TestBase with Mockito with Logging {

	trait Environment {

		val studentMember = Fixtures.student()
		val year = studentMember.mostSignificantCourseDetails.get.latestStudentCourseYearDetails.academicYear

	}

	@Transactional
	@Test def testVisaUpdatesAreApplied() {
		new Environment {

			val casUsageImporter = smartMock[CasUsageImporter]
			casUsageImporter.isCasUsed(studentMember.universityId) returns false

			val tier4VisaImporter = smartMock[Tier4VisaImporter]
			tier4VisaImporter.hasTier4Visa(studentMember.universityId) returns false

			val command = ImportTier4ForStudentCommand(studentMember, year)
			command.casUsageImporter = casUsageImporter
			command.tier4VisaImporter = tier4VisaImporter
			command.studentCourseYearDetailsDao = smartMock[StudentCourseYearDetailsDao]
			command.applyInternal

			studentMember.casUsed should be (Some(false))
			studentMember.hasTier4Visa should be (Some(false))
		}
	}


	@Transactional
	@Test def testVisaUpdatesApplyGoingForward() {
		new Environment {
			val scd = studentMember.mostSignificantCourse;
			val currentScyd = scd.latestStudentCourseYearDetails
			currentScyd.yearOfStudy = 2
			val lastScyd = Fixtures.studentCourseYearDetails(AcademicYear.guessByDate(DateTime.now.minusYears(1)), null, 1, scd)
			scd.attachStudentCourseYearDetails(lastScyd);
			val nextScyd = Fixtures.studentCourseYearDetails(AcademicYear.guessByDate(DateTime.now.plusYears(1)), null, 3, scd)
			scd.attachStudentCourseYearDetails(nextScyd);

			val casUsageImporter = smartMock[CasUsageImporter]
			casUsageImporter.isCasUsed(studentMember.universityId) returns true

			val tier4VisaImporter = smartMock[Tier4VisaImporter]
			tier4VisaImporter.hasTier4Visa(studentMember.universityId) returns true

			val command = ImportTier4ForStudentCommand(studentMember, year)
			command.casUsageImporter = casUsageImporter
			command.tier4VisaImporter = tier4VisaImporter
			command.studentCourseYearDetailsDao = smartMock[StudentCourseYearDetailsDao]
			command.applyInternal

			studentMember.casUsed should be (Some(true))
			studentMember.hasTier4Visa should be (Some(true))
			lastScyd.tier4Visa.booleanValue() should be (false)
			lastScyd.casUsed.booleanValue() should be (false)
			currentScyd.tier4Visa.booleanValue() should be (true)
			currentScyd.casUsed.booleanValue() should be (true)
			nextScyd.tier4Visa.booleanValue() should be (true)
			nextScyd.casUsed.booleanValue() should be (true)
		}
	}
}
