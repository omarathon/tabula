package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.services.objectstore.ObjectStorageService
import uk.ac.warwick.tabula.{Mockito, PersistenceTestBase}
import org.junit.{Before, After}
import uk.ac.warwick.tabula.data.model.FileAttachment
import java.io.{FileInputStream, InputStream, ByteArrayInputStream, File}
import org.joda.time.DateTime
import org.springframework.util.FileCopyUtils
import org.joda.time.DateTimeConstants
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.util.files.hash.FileHasher
import uk.ac.warwick.util.files.hash.impl.SHAFileHasher

// scalastyle:off magic.number
@Transactional
class FileDaoTest extends PersistenceTestBase with Mockito {

	val dao = new FileDao
	val objectStorageService = smartMock[ObjectStorageService]

	@Before def setup() {
		dao.objectStorageService = objectStorageService
		dao.sessionFactory = sessionFactory
	}

	@Test def deletingTemporaryFiles = withFakeTime(new DateTime(2012, DateTimeConstants.JANUARY, 15, 1, 0, 0, 0)) {
		transactional { transactionStatus =>
			for (i <- 0 to 50) {
				val attachment = new FileAttachment
				attachment.dateUploaded = new DateTime().plusHours(1).minusDays(i)
				attachment.uploadedData = new ByteArrayInputStream("This is the best file ever".getBytes)
				attachment.fileDao = dao
				attachment.objectStorageService = objectStorageService
				dao.saveTemporary(attachment)
			}
		}
		transactional { transactionStatus =>
			dao.deleteOldTemporaryFiles should be (36) // 50 files, 14 days of leeway
		}
	}

	@After def bangtidy() { transactional { tx =>
		session.createQuery("delete from FileAttachment").executeUpdate()
	}}

	@Test def crud = transactional { tx =>
		val attachments = for (i <- 1 to 10) yield {
			val attachment = new FileAttachment
			attachment.dateUploaded = new DateTime(2013, DateTimeConstants.FEBRUARY, i, 1, 0, 0, 0)
			attachment.uploadedData = new ByteArrayInputStream("This is the best file ever".getBytes)
			attachment.fileDao = dao
			attachment.objectStorageService = objectStorageService
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

	@Test
	def save() {
		transactional { tx =>
			val attachment = new FileAttachment("file.txt")
			val string = "Doe, a deer, a female deer"
			val bytes = string.getBytes("UTF-8")
			attachment.fileDao = dao
			attachment.objectStorageService = objectStorageService
			attachment.uploadedDataLength = bytes.length
			attachment.uploadedData = new ByteArrayInputStream(bytes)

			dao.saveTemporary(attachment)
			verify(objectStorageService, times(1)).push(attachment.id, attachment.uploadedData, ObjectStorageService.Metadata(26, "application/octet-stream", Some("4931c07015e50baf297aae2ce8571da15c0f2380")))

			attachment.id should not be (null)

			session.flush()
			session.clear()

			dao.getFileById(attachment.id) match {
				case Some(loadedAttachment:FileAttachment) => {
					//val blob = loadedAttachment.data
					loadedAttachment.fileDao = dao
					loadedAttachment.objectStorageService = objectStorageService
					loadedAttachment.objectStorageService.fetch(attachment.id) returns Some(new ByteArrayInputStream(bytes))

					val data = readStream(loadedAttachment.dataStream, "UTF-8")
					data should be (string)
				}
				case None => fail("nope")
			}
		}
	}

	private def readStream(is:InputStream, encoding:String) = new String(FileCopyUtils.copyToByteArray(is), encoding)

}