package uk.ac.warwick.tabula.scheduling.commands.imports

import org.joda.time.DateTime
import org.springframework.transaction.annotation.Transactional

import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.{FileDao, MemberDao}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.services.{DisabilityInfo, AwardInfo}


class ImportDisabilitiesCommandTest extends AppContextTestBase with Mockito with Logging {

	@Transactional
	@Test def testImportDisabilitiesCommand() {
		val info = new DisabilityInfo("72", "HERON", "You spear fish like a demon, but struggle in academic environments")

		// test command
		val command = new ImportDisabilitiesCommand(info)
		val (disability, result) = command.applyInternal
		disability.code should be ("72")
		disability.shortName should be ("HERON")
		disability.sitsDefinition should be ("You spear fish like a demon, but struggle in academic environments")
		disability.lastUpdatedDate.dayOfMonth should be ((new DateTime).dayOfMonth)
		disability.tabulaDefinition should be (null)
		result should be (ImportAcademicInformationCommand.ImportResult(added = 1))
	}
}
