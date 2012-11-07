package uk.ac.warwick.courses.services.turnitin

import uk.ac.warwick.courses.TestBase
import uk.ac.warwick.courses.data.model.FileAttachment

class TurnitinSubmissionInfoTest extends TestBase {

	@Test
	def matchingAttachments {
		val attachment = new FileAttachment
		attachment.id = "12345"
			
		newSubmissionInfoWithTitle("12345").matches(attachment) should be(true)
		newSubmissionInfoWithTitle("12346").matches(attachment) should be(false)
		newSubmissionInfoWithTitle("what on earth is this").matches(attachment) should be(false)
	}

	private def newSubmissionInfoWithTitle(title: String) =
		new TurnitinSubmissionInfo(
			DocumentId("0"),
			title,
			"0123456",
			-1,
			None,
			None,
			None,
			None)

}