package uk.ac.warwick.tabula.scheduling.commands.imports

import org.joda.time.DateTime
import org.springframework.transaction.annotation.Transactional

import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.{FileDao, MemberDao}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.services.AwardInfo


class ImportAwardCommandTest extends AppContextTestBase with Mockito with Logging {

	@Transactional
	@Test def testImportAwardCommand() {
		val info = new AwardInfo("BSC", "BSc", "Bachelor of Science")

		// test command
		val command = new ImportAwardCommand(info)
		val (award, result) = command.applyInternal
		award.code should be ("BSC")
		award.shortName should be ("BSc")
		award.name should be ("Bachelor of Science")
		award.lastUpdatedDate.dayOfMonth should be ((new DateTime).dayOfMonth)
		result should be (ImportAcademicInformationCommand.ImportResult(added = 1))

	}

}
