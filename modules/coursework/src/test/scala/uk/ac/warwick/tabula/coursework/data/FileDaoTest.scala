package uk.ac.warwick.tabula.coursework.data

import uk.ac.warwick.tabula.coursework.AppContextTestBase
import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.coursework.data.model.FileAttachment
import java.io.ByteArrayInputStream
import org.joda.time.DateTime

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
	
}