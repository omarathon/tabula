package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase
import org.junit.Test
import javax.persistence.Entity
import org.hibernate.annotations.AccessType
import org.junit.Test
import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.JavaImports._

class SubmissionTest extends PersistenceTestBase {
	@Test def allAttachments {
		val submission = new Submission
		submission.allAttachments.size should be (0)
	}
	
	@Test def deleteFileAttachmentOnDelete {
		// TAB-667
		val orphanAttachment = transactional { tx =>
			val attachment = new FileAttachment
			
			session.save(attachment)
			attachment
		}
		
		val (submission, submissionAttachment) = transactional { tx => 
			val submission = new Submission(universityId = "0000001")
			submission.userId = "steve"
			
			val assignment = new Assignment
			session.save(assignment)
			
			submission.assignment = assignment
			
			session.save(submission)
			
			val attachment = new FileAttachment
			val ssv = SavedSubmissionValue.withAttachments(submission, "name", JSet(attachment))
			submission.values.add(ssv)
			
			session.saveOrUpdate(submission)
			(submission, attachment)
		}
		
		// Ensure everything's been persisted
		orphanAttachment.id should not be (null)
		submission.id should not be (null)
		submissionAttachment.id should not be (null)
		
		// Can fetch everything from db
		transactional { tx => 
			session.get(classOf[FileAttachment], orphanAttachment.id) should be (orphanAttachment)
			session.get(classOf[Submission], submission.id) should be (submission)
			session.get(classOf[FileAttachment], submissionAttachment.id) should be (submissionAttachment)
		}
		
		transactional { tx => session.delete(submission) }
		
		// Ensure we can't fetch the submission or attachment, but all the other objects are returned
		transactional { tx => 
			session.get(classOf[FileAttachment], orphanAttachment.id) should be (orphanAttachment)
			session.get(classOf[Submission], submission.id) should be (null)
			session.get(classOf[FileAttachment], submissionAttachment.id) should be (null)
		}
	}
}