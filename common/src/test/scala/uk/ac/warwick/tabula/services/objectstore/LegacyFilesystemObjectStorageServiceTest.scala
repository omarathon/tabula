package uk.ac.warwick.tabula.services.objectstore

import java.io.File
import java.nio.charset.StandardCharsets

import com.google.common.io.Files
import uk.ac.warwick.tabula.{Mockito, TestBase}

class LegacyFilesystemObjectStorageServiceTest extends TestBase with Mockito {

	private trait Fixture {
		val attachmentDir: File = createTemporaryDirectory()

		val service = new LegacyFilesystemObjectStorageService(attachmentDir)
	}

	/*
	 * TAB-202 changes the storage to split the path every 2 characters
	 * instead of every 4. This checks that we work with 2 characters for new
	 * data but can still find existing data stored under the old location.
	 */
	@Test
	def compatDirectorySplit(): Unit = new Fixture {
		// Create some fake files, of new and old format
		val paths = Seq(
			"aaaa/bbbb/dddd/eeee",
			"aaaa/bbbb/cccc/dddd",
			"aa/aa/bb/bb/cc/cc/ef/ef"
		)

		for (path <- paths) {
			val file = new File(attachmentDir, path)
			assert(file.getParentFile.exists || file.getParentFile.mkdirs())
			assert(file.createNewFile())
			Files.asByteSink(file).asCharSink(StandardCharsets.UTF_8).write("content") // just to make isEmpty return false
		}

		service.fetch("aaaabbbbccccdddd").isEmpty should be(false)
		service.fetch("aaaabbbbddddeeee").isEmpty should be(false)
		service.fetch("aaaabbbbccccefef").isEmpty should be(false)
	}

}
