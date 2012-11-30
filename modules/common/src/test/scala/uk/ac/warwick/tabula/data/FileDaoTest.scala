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

class FileDaoTest extends AppContextTestBase {

	@Autowired var dao:FileDao =_

	@Test def deletingTemporaryFiles {
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

			getRelativePath( dao.getData("aaaabbbbccccdddd").orNull ) should be ("/aaaa/bbbb/cccc/dddd")
			getRelativePath( dao.getData("aaaabbbbddddeeee").orNull ) should be ("/aaaa/bbbb/dddd/eeee")
			getRelativePath( dao.getData("aaaabbbbccccefef").orNull ) should be ("/aa/aa/bb/bb/cc/cc/ef/ef")

		}
	}
	
}