package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase
import org.junit.Test
import javax.persistence.Entity
import org.hibernate.annotations.AccessType
import org.junit.Test

class SubmissionTest extends TestBase {
	@Test def allAttachments {
		val submission = new Submission
		submission.allAttachments.size should be (0)
	}
}