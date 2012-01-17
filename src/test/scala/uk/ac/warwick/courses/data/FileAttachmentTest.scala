package uk.ac.warwick.courses.data
import org.scalatest.junit.ShouldMatchersForJUnit
import uk.ac.warwick.courses.AppContextTestBase
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.model.FileAttachment
import java.io.ByteArrayInputStream
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.FileCopyUtils
import java.io.InputStream

class FileAttachmentTest extends AppContextTestBase {
	@Autowired var dao:FileDao =_
	
	@Transactional
	@Test def save {
		val attachment = new FileAttachment("file.txt")
		val string = "Doe, a deer, a female deer"
		val bytes = string.getBytes("UTF-8")
		attachment.uploadedDataLength = bytes.length
		attachment.uploadedData = new ByteArrayInputStream(bytes)
		dao.saveTemporary(attachment)
		
		attachment.id should not be (null)
		
		session.flush
		session.clear
		
		dao.getFileById(attachment.id) match {
			case Some(loadedAttachment:FileAttachment) => {
				//val blob = loadedAttachment.data
				val data = readStream(loadedAttachment.dataStream, "UTF-8")
				data should be (string)
			}
		}
		
	}
	
	def readStream(is:InputStream, encoding:String) = new String(FileCopyUtils.copyToByteArray(is), encoding)
}