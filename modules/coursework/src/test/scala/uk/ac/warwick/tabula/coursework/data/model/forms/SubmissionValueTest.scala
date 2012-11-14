package uk.ac.warwick.tabula.coursework.data.model.forms

import uk.ac.warwick.tabula.coursework.TestBase
import org.junit.Test
import uk.ac.warwick.tabula.coursework.data.model.FileAttachment
import uk.ac.warwick.tabula.coursework.data.model.SavedSubmissionValue



class SubmissionValueTest extends TestBase {
	@Test def makePermanentOnPersist {
		val field = new FileField()
		val value = new FileSubmissionValue(field)
		val attachment = new FileAttachment()
		attachment.temporary.booleanValue should be (true)
		
		value.file.attached.add(attachment)
		
		val saved = new SavedSubmissionValue()
		value.persist(saved)
		saved.attachments.size should be (1)
		saved.attachments.iterator.next.temporary.booleanValue should be (false)
	}
}