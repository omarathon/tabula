package uk.ac.warwick.tabula.data.model.forms
import scala.collection.JavaConversions._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.data.model.{Submission, Assignment}
import uk.ac.warwick.tabula.data.model.FileAttachment

class ExtensionTest extends PersistenceTestBase {

  @Test def testExtension {

    val assignment = new Assignment
    assignment.closeDate = new DateTime(2012, 7, 12, 12, 0)

    val extension = new Extension()
    extension.universityId = "1170836"
    extension.userId = "cuslaj"
    extension.expiryDate = new DateTime(2012, 8, 12, 12, 0)
    extension.reason = "My hands have turned to flippers. Like the ones that dolphins have. It makes writing and typing super hard. Pity me."
    extension.approvalComments = "That sounds awful. Have an extra month. By then you should be able to write as well as any Cetacea."
    extension.approved = true
    extension.approvedOn = new DateTime(2012, 7, 22, 14, 42)

    assignment.extensions add extension

    withFakeTime(dateTime(2012, 8)) {
      assignment.isWithinExtension("cuslaj") should be (true)  // has an extension so can submit
      assignment.isWithinExtension("cuscao") should be (false) // cannot submit
    }

    withFakeTime(dateTime(2012, 7)) {
      for (i <- 1 to 10) {
        val newSubmission = new Submission(universityId = idFormat(i))
        newSubmission.submittedDate = new DateTime
        assignment.submissions add newSubmission
      }
    }

    withFakeTime(dateTime(2012, 8)) {
      for (i <- 11 to 15) {
        val newSubmission = new Submission(universityId = idFormat(i))
        newSubmission.submittedDate = new DateTime
        assignment.submissions add newSubmission
      }
      val newSubmission = new Submission(universityId = idFormat(1170836))
      newSubmission.userId = "cuslaj"
      newSubmission.submittedDate = new DateTime
      assignment.submissions add newSubmission
    }

    val lateSubmissions = assignment.submissions filter (assignment.isLate(_)) map (_.universityId)
    lateSubmissions should be ((11 to 15) map idFormat)

  }
	
	@Test def deleteFileAttachmentOnDelete {
		// TAB-667
		val orphanAttachment = transactional { tx =>
			val attachment = new FileAttachment
			
			session.save(attachment)
			attachment
		}
		
		val (extension, extensionAttachment) = transactional { tx =>
			val extension = new Extension(universityId = idFormat(1))
			extension.userId = "steve"
				
			val assignment = new Assignment
			session.save(assignment)
			
			extension.assignment = assignment
			
			val attachment = new FileAttachment
			extension.addAttachment(attachment)
			
			session.save(extension)
			(extension, attachment)
		}
		
		// Ensure everything's been persisted
		orphanAttachment.id should not be (null)
		extension.id should not be (null)
		extensionAttachment.id should not be (null)
		
		// Can fetch everything from db
		transactional { tx => 
			session.get(classOf[FileAttachment], orphanAttachment.id) should be (orphanAttachment)
			session.get(classOf[Extension], extension.id) should be (extension)
			session.get(classOf[FileAttachment], extensionAttachment.id) should be (extensionAttachment)
		}
		
		transactional { tx => session.delete(extension) }
		
		// Ensure we can't fetch the extension or attachment, but all the other objects are returned
		transactional { tx => 
			session.get(classOf[FileAttachment], orphanAttachment.id) should be (orphanAttachment)
			session.get(classOf[Extension], extension.id) should be (null)
			session.get(classOf[FileAttachment], extensionAttachment.id) should be (null)
		}
	}

  /** Zero-pad integer to a 7 digit string */
  def idFormat(i:Int) = "%07d" format i
  
  @Test def flags {
	  val extension = new Extension
	  
	  extension.isManual should be (true)
	  extension.isAwaitingApproval should be (false)
	   
	  extension.requestedOn = DateTime.now
	  
	  extension.isManual should be (false)
	  extension.isAwaitingApproval should be (true)
	   
	  extension.approved = true
	  
	  extension.isAwaitingApproval should be (false)
  }
}
