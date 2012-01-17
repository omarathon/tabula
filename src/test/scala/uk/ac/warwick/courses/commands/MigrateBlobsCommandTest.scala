package uk.ac.warwick.courses.commands

import uk.ac.warwick.courses.AppContextTestBase
import org.junit.Test
import uk.ac.warwick.courses.data.model.FileAttachment
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.FileDao
import java.io.ByteArrayInputStream
import org.hibernate.criterion.Restrictions
import org.springframework.transaction.annotation.Transactional

class MigrateBlobsCommandTest extends AppContextTestBase {
  
	@Autowired var fileDao:FileDao =_
  
	@Transactional
	@Test def migrate {
	    val helper = session.getLobHelper()
	    for (i <- Range(0,3)) {
	    	var attachment = new FileAttachment
	    	attachment.name = "blobfile"+i
	    	attachment.blob = helper.createBlob(Array(1,2,3,4,5,6,7,8))
	    	attachment.temporary = true
	    	session.save(attachment)
	    }
	    for (i <- Range(0,3)) {
	    	var attachment = new FileAttachment
	    	attachment.name = "blobfile"+i
	    	attachment.uploadedData = new ByteArrayInputStream(Array(1,2,3,4,5,6,7,8))
	    	fileDao.saveTemporary(attachment)
	    }
	    session.flush
	    
	    fileBasedAttachments.size() should be(3)
	  
		val command = new MigrateBlobsCommand
		command.apply
		
		fileBasedAttachments.size() should be(6)
	}
	
	def fileBasedAttachments = session.createCriteria(classOf[FileAttachment])
			.add(Restrictions.isNull("blob"))
			.list()
}