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
import uk.ac.warwick.tabula.scheduling.services.SupervisorImporter
import uk.ac.warwick.tabula.helpers.Logging
import org.joda.time.DateTime
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.scheduling.services.CasUsageImporter
import uk.ac.warwick.tabula.data.StudentCourseYearDetailsDao


class ImportCasUsageForStudentCommandTest extends AppContextTestBase with Mockito with Logging {

	trait Environment {

		val studentMember = Fixtures.student()
		val year = studentMember.mostSignificantCourseDetails.get.latestStudentCourseYearDetails.academicYear

	}

	@Transactional
	@Test def testCaptureValidSupervisor() {
		new Environment {

			val importer = smartMock[CasUsageImporter]
			importer.isCasUsed(studentMember.universityId) returns false

			val command = new ImportCasUsageForStudentCommand(studentMember, year)
			command.casUsageImporter = importer
			command.scydDao = smartMock[StudentCourseYearDetailsDao]
			command.applyInternal

			studentMember.casUsed.booleanValue() should be (false)

		}
	}
}
