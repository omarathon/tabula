package uk.ac.warwick.tabula.data.model.forms
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.model.SavedSubmissionValue
import uk.ac.warwick.tabula.data.model.Submission

class SubmissionValueTest extends PersistenceTestBase {
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
	
	@Test def deleteFileAttachmentOnDelete {
		// TAB-667
		val orphanAttachment = transactional { tx =>
			val attachment = new FileAttachment
			
			session.save(attachment)
			attachment
		}
		
		val (ssv, ssvAttachment1, ssvAttachment2) = transactional { tx =>
			val submission = new Submission(universityId = "0000001")
			submission.userId = "steve"
				
			val assignment = new Assignment
			session.save(assignment)
			
			submission.assignment = assignment
			
			session.save(submission)
			
			val attachment1 = new FileAttachment
			val attachment2 = new FileAttachment
			
			val ssv = SavedSubmissionValue.withAttachments(submission, "name", JSet(attachment1, attachment2))
			
			session.save(ssv)
			(ssv, attachment1, attachment2)
		}
		
		// Ensure everything's been persisted
		orphanAttachment.id should not be (null)
		ssv.id should not be (null)
		ssvAttachment1.id should not be (null)
		ssvAttachment2.id should not be (null)
		
		// Can fetch everything from db
		transactional { tx => 
			session.get(classOf[FileAttachment], orphanAttachment.id) should be (orphanAttachment)
			session.get(classOf[SavedSubmissionValue], ssv.id) should be (ssv)
			session.get(classOf[FileAttachment], ssvAttachment1.id) should be (ssvAttachment1)
			session.get(classOf[FileAttachment], ssvAttachment2.id) should be (ssvAttachment2)
		}
		
		transactional { tx => session.delete(ssv) }
		
		// Ensure we can't fetch the extension or attachment, but all the other objects are returned
		transactional { tx => 
			session.get(classOf[FileAttachment], orphanAttachment.id) should be (orphanAttachment)
			session.get(classOf[SavedSubmissionValue], ssv.id) should be (null)
			session.get(classOf[FileAttachment], ssvAttachment1.id) should be (null)
			session.get(classOf[FileAttachment], ssvAttachment2.id) should be (null)
		}
	}
}