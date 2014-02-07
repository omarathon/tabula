package uk.ac.warwick.tabula.scheduling.commands.imports

import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.model.DegreeType.Postgraduate
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.scheduling.services.{Tier4VisaImporter, SupervisorImporter, CasUsageImporter}
import uk.ac.warwick.tabula.helpers.Logging
import org.joda.time.DateTime
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.data.StudentCourseYearDetailsDao
import uk.ac.warwick.tabula.TestBase

class ImportTier4ForStudentCommandTest extends TestBase with Mockito with Logging {

	trait Environment {

		val studentMember = Fixtures.student()
		val year = studentMember.mostSignificantCourseDetails.get.latestStudentCourseYearDetails.academicYear

	}

	@Transactional
	@Test def testCaptureValidSupervisor() {
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
}
