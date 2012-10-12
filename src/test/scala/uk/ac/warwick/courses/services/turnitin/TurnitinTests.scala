package uk.ac.warwick.courses.services.turnitin

import uk.ac.warwick.courses.TestBase

class TurnitinTests extends TestBase {

	@Test
	def documentTitle {
		DocumentTitle("12345", "ext") match {
			case DocumentTitle(id, ext) => {
				id should be("12345")
				ext should be("ext")
			}
		}
		AnyDocumentTitle("12345.ext") match {
			case DocumentTitle(id, ext) => {
				id should be("12345")
				ext should be("ext")
			}
		}
	}

}