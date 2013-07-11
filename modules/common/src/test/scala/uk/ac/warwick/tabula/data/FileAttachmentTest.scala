package uk.ac.warwick.tabula.data
import uk.ac.warwick.tabula.{TestBase, AppContextTestBase}
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.FileAttachment
import java.io.ByteArrayInputStream
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.FileCopyUtils
import java.io.InputStream
import javax.persistence.Entity
import org.hibernate.annotations.AccessType
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional

class FileAttachmentTest extends TestBase {

	/** 
	 * HFC-136 
	 * Filenames can be prefixed with whitespace, preventing download
	 */
	@Test
	def untrimmedFilenames {
		new FileAttachment(" 0123456.docx").name should be ("0123456.docx")
		new FileAttachment(null).name should be (null)
	}

	@Test
	def badWindowsFilenames {
		new FileAttachment(""" \Fun: good or bad? <You|decide>/.docx""").name should be ("Fun good or bad Youdecide.docx")
	}

	@Test
	def removeSemicolons() {
		new FileAttachment("We've had our fun; now let's go home.xlsx").name should be ("We've had our fun now let's go home.xlsx")
	}

}