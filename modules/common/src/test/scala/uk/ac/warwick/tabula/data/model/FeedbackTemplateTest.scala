package uk.ac.warwick.tabula.data.model

import scala.util.Random
import uk.ac.warwick.tabula.PersistenceTestBase

class FeedbackTemplateTest extends PersistenceTestBase {
	
	@Test def deleteFileAttachmentOnDelete {
		// TAB-667
		val orphanAttachment = transactional { tx =>
			val attachment = new FileAttachment
			
			session.save(attachment)
			attachment
		}
		
		val (feedbackTemplate, feedbackAttachment) = transactional { tx => 
			val feedbackTemplate = new FeedbackTemplate
			
			val attachment = new FileAttachment
			feedbackTemplate.attachFile(attachment)
			
			session.save(feedbackTemplate)
			(feedbackTemplate, attachment)
		}
		
		// Ensure everything's been persisted
		orphanAttachment.id should not be (null)
		feedbackTemplate.id should not be (null)
		feedbackAttachment.id should not be (null)
		
		// Can fetch everything from db
		transactional { tx => 
			session.get(classOf[FileAttachment], orphanAttachment.id) should be (orphanAttachment)
			session.get(classOf[FeedbackTemplate], feedbackTemplate.id) should be (feedbackTemplate)
			session.get(classOf[FileAttachment], feedbackAttachment.id) should be (feedbackAttachment)
		}
		
		transactional { tx => session.delete(feedbackTemplate) }
		
		// Ensure we can't fetch the FeedbackTemplate or attachment, but all the other objects are returned
		transactional { tx => 
			session.get(classOf[FileAttachment], orphanAttachment.id) should be (orphanAttachment)
			session.get(classOf[FeedbackTemplate], feedbackTemplate.id) should be (null)
			session.get(classOf[FileAttachment], feedbackAttachment.id) should be (null)
		}
	}
	
}