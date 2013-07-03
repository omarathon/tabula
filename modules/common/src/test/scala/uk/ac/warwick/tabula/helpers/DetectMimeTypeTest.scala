package uk.ac.warwick.tabula.helpers

import org.springframework.core.io.ClassPathResource
import uk.ac.warwick.tabula.TestBase

class DetectMimeTypeTest extends TestBase {

	@Test
	def detect() {
		val file = new ClassPathResource("/feedback1.zip").getFile
		val mimeType = DetectMimeType.detectMimeType(file)
		mimeType should be ("application/zip")
	}

}
