package uk.ac.warwick.courses.data.model

import uk.ac.warwick.courses.TestBase
import org.junit.Test

class SubmissionTest extends TestBase {
	@Test def allAttachments {
		val submission = new Submission
		submission.allAttachments.size should be (0)
	}
}