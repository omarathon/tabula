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