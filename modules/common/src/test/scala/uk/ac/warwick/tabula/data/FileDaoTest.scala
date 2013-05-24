package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.AppContextTestBase
import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import uk.ac.warwick.tabula.data.model.FileAttachment
import java.io.ByteArrayInputStream
import org.joda.time.DateTime
import javax.persistence.Entity
import org.hibernate.annotations.AccessType
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.io.File
import org.springframework.util.FileCopyUtils
import org.joda.time.DateTimeConstants
import org.junit.After
import org.springframework.transaction.annotation.Transactional

// scalastyle:off magic.number
@Transactional
class FileDaoTest extends AppContextTestBase {

	@Autowired var dao:FileDao =_

	@Test def deletingTemporaryFiles = withFakeTime(new DateTime(2012, DateTimeConstants.JANUARY, 15, 1, 0, 0, 0)) {
		transactional { transactionStatus =>
			dao.attachmentDir = createTemporaryDirectory
			dao.attachmentDir.list.size should be (0)
			for (i <- Range(0,10)) {
				val attachment = new FileAttachment
				attachment.dateUploaded = new DateTime().plusHours(1).minusDays(i)
				attachment.uploadedData = new ByteArrayInputStream("This is the best file ever".getBytes)
				dao.saveTemporary(attachment)
			}
		}
		dao.deleteOldTemporaryFiles should be (7)
	}
	
	@After def bangtidy { transactional { tx => 
		session.createQuery("delete from FileAttachment").executeUpdate() 
	}}
	
	@Test def crud = transactional { tx => 
		dao.attachmentDir = createTemporaryDirectory
		dao.attachmentDir.list.size should be (0)
		val attachments = for (i <- 1 to 10) yield {
			val attachment = new FileAttachment
			attachment.dateUploaded = new DateTime(2013, DateTimeConstants.FEBRUARY, i, 1, 0, 0, 0)
			attachment.uploadedData = new ByteArrayInputStream("This is the best file ever".getBytes)
			dao.savePermanent(attachment)
			
			attachment.hash should be ("f95a27f06df98ba26182c22e277af960c0be9be6")

			attachment
		}
		
		for (attachment <- attachments) {
			dao.getFileById(attachment.id) should be (Some(attachment))
			dao.getFileByStrippedId(attachment.id.replaceAll("\\-", "")) should be (Some(attachment))
			dao.getFilesCreatedOn(attachment.dateUploaded, 10, "") should be (Seq(attachment))
			dao.getFilesCreatedOn(attachment.dateUploaded, 10, attachment.id) should be (Seq())
		}
		
		dao.getFilesCreatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), 1) should be (Seq(attachments.head))		
		dao.getFilesCreatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 5, 0, 0, 0, 0), 1) should be (Seq(attachments(4)))
		dao.getFilesCreatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 5, 0, 0, 0, 0), 10) should be (attachments.slice(4, 10))
		
		dao.getAllFileIds() should be ((attachments map { _.id }).toSet)
		dao.getAllFileIds(Some(new DateTime(2013, DateTimeConstants.FEBRUARY, 5, 0, 0, 0, 0))) should be ((attachments.slice(0, 4) map { _.id }).toSet)
	}

	/*
	 * TAB-202 changes the storage to split the path every 2 characters
	 * instead of every 4. This checks that we work with 2 characters for new
	 * data but can still find existing data stored under the old location.
	 */
	@Test
	def compatDirectorySplit {
		transactional { tx =>
			dao.attachmentDir = createTemporaryDirectory

			// Create some fake files, of new and old format
			val paths = Seq(
					"aaaa/bbbb/dddd/eeee",
					"aaaa/bbbb/cccc/dddd",
					"aa/aa/bb/bb/cc/cc/ef/ef")
			for (path <- paths) {
				val file = new File(dao.attachmentDir, path)
				assert( file.getParentFile.exists || file.getParentFile.mkdirs() )
				assert( file.createNewFile() )
			}

			def getRelativePath(file: File) = {
				val prefix = dao.attachmentDir.getAbsolutePath()
				file.getAbsolutePath().replace(prefix, "")
			}

			getRelativePath( dao.getData("aaaabbbbccccdddd").orNull ) should be (File.separator + "aaaa" + File.separator + "bbbb" + File.separator  + "cccc" + File.separator + "dddd")
			getRelativePath( dao.getData("aaaabbbbddddeeee").orNull ) should be (File.separator + "aaaa" + File.separator + "bbbb" + File.separator + "dddd" + File.separator + "eeee")
			getRelativePath( dao.getData("aaaabbbbccccefef").orNull ) should be (File.separator + "aa" + File.separator + "aa" + File.separator + "bb" + File.separator + "bb" + File.separator + "cc" + File.separator + "cc" + File.separator + "ef" + File.separator + "ef")

		}
	}
	
}