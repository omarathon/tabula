package uk.ac.warwick.courses.data.model.forms

import uk.ac.warwick.courses.TestBase
import org.junit.Test
import uk.ac.warwick.courses.data.model.FileAttachment
import uk.ac.warwick.courses.data.model.SavedSubmissionValue


class SubmissionValueTest extends TestBase {
	@Test def makePermanentOnPersist {
		val field = new FileField()
		val value = new FileSubmissionValue(field)
		val attachment = new FileAttachment()
		attachment.temporary should be (true)
		
		value.file.attached.add(attachment)
		
		val saved = new SavedSubmissionValue()
		value.persist(saved)
		saved.attachments.size should be (1)
		saved.attachments.iterator.next.temporary should be (false)
	}
}