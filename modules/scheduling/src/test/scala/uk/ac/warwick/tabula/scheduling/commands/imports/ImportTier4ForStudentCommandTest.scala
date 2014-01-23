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
import uk.ac.warwick.tabula.scheduling.services.Tier4RequirementImporter


class ImportTier4ForStudentCommandTest extends AppContextTestBase with Mockito with Logging {

	trait Environment {

		val studentMember = Fixtures.student()
		val year = studentMember.mostSignificantCourseDetails.get.latestStudentCourseYearDetails.academicYear

	}

	@Transactional
	@Test def testTier4Requirement() {
		new Environment {

			val importer = smartMock[Tier4RequirementImporter]
			importer.hasTier4Requirement(studentMember.universityId) returns true

			val command = new ImportTier4ForStudentCommand()
			command.student = studentMember
			command.requirementImporter = importer
			command.memberDao = smartMock[MemberDao]
			command.applyInternal

			studentMember.tier4VisaRequirement.booleanValue() should be (true)

		}
	}
}
