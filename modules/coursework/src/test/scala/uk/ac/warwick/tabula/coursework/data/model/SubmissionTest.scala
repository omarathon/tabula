package uk.ac.warwick.tabula.coursework.data.model

import uk.ac.warwick.tabula.coursework.TestBase


import org.junit.Test

class SubmissionTest extends TestBase {
	@Test def allAttachments {
		val submission = new Submission
		submission.allAttachments.size should be (0)
	}
}