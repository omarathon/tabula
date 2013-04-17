package uk.ac.warwick.tabula.data
import uk.ac.warwick.tabula.AppContextTestBase
import org.junit.Test
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.FileAttachment
import java.io.ByteArrayInputStream

import org.springframework.util.FileCopyUtils
import java.io.InputStream
import javax.persistence.Entity
import org.hibernate.annotations.AccessType
import org.junit.Test
import org.junit.runner.RunWith

import org.springframework.stereotype.Repository
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner


class FileAttachmentTest extends AppContextTestBase {
	lazy val dao = Wire[FileDao]
	
	/** 
	 * HFC-136 
	 * Filenames can be prefixed with whitespace, preventing download
	 */
	@Test def untrimmedFilenames {
		new FileAttachment(" 0123456.docx").name should be ("0123456.docx")
		new FileAttachment(null).name should be (null)
	}
	
	@Test def badWindowsFilenames {
		new FileAttachment(""" \Fun: good or bad? <You|decide>/.docx""").name should be ("Fun good or bad Youdecide.docx")
	}
	
	@Test def save = transactional { tx =>
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
			case None => fail("nope")
		}
		
	}
	
	def readStream(is:InputStream, encoding:String) = new String(FileCopyUtils.copyToByteArray(is), encoding)
}