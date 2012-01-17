package uk.ac.warwick.courses.commands

import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.data.model.FileAttachment
import org.hibernate.criterion.Restrictions
import collection.JavaConversions._
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.FileDao
import org.springframework.util.FileCopyUtils
import java.io.FileOutputStream
import org.springframework.transaction.annotation.Transactional

@Configurable
class MigrateBlobsCommand extends Command[Unit] with Daoisms {
	
	var blobsConverted = 0
	
	@Autowired var fileDao:FileDao = _
	
	def apply {
		val blobs = session.newCriteria[FileAttachment]
				.add(Restrictions.isNotNull("data"))
				.list
				
		for (attachment <- blobs) 
			migrate(attachment)
		
	}
	
	@Transactional
	def migrate(attachment:FileAttachment) {
		val inputStream = attachment.dataStream
		val targetFile = attachment.file
		FileCopyUtils.copy(inputStream, new FileOutputStream(targetFile))
		attachment.blob = null
	}
	
	override def describe(d:Description) = {}
	
	override def describeResult(d:Description) = {
		"blobsConverted" -> blobsConverted
	}
	
}