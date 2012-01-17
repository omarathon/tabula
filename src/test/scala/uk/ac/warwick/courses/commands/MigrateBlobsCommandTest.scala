package uk.ac.warwick.courses.commands

import uk.ac.warwick.courses.AppContextTestBase
import org.junit.Test

class MigrateBlobsCommandTest extends AppContextTestBase {
	@Test def migrate {
		val command = new MigrateBlobsCommand
		
		command.apply
	}
}