package uk.ac.warwick.tabula.data.model

import scala.util.Random
import uk.ac.warwick.tabula.PersistenceTestBase

class OriginalityReportTest extends PersistenceTestBase {
		
	@Test def deleteFileAttachmentOnDelete {
		// TAB-667
		val orphanAttachment = transactional { tx =>
			val attachment = new FileAttachment
			
			session.save(attachment)
			attachment
		}
		
		val (report, reportAttachment) = transactional { tx => 
			val report = new OriginalityReport
			
			val attachment = new FileAttachment
			report.attachment = attachment
			
			session.save(report)
			(report, attachment)
		}
		
		// Ensure everything's been persisted
		orphanAttachment.id should not be (null)
		report.id should not be (null)
		reportAttachment.id should not be (null)
		
		// Can fetch everything from db
		transactional { tx => 
			session.get(classOf[FileAttachment], orphanAttachment.id) should be (orphanAttachment)
			session.get(classOf[OriginalityReport], report.id) should be (report)
			session.get(classOf[FileAttachment], reportAttachment.id) should be (reportAttachment)
		}
		
		transactional { tx => session.delete(report) }
		
		// Ensure we can't fetch the report or attachment, but all the other objects are returned
		transactional { tx => 
			session.get(classOf[FileAttachment], orphanAttachment.id) should be (orphanAttachment)
			session.get(classOf[OriginalityReport], report.id) should be (null)
			session.get(classOf[FileAttachment], reportAttachment.id) should be (null)
		}
	}
}