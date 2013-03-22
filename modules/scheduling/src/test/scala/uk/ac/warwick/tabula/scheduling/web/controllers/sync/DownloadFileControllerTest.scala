package uk.ac.warwick.tabula.scheduling.web.controllers.sync

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.FileDao
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.json.JSONObject
import uk.ac.warwick.tabula.scheduling.services.SHAMessageAuthenticationCodeGenerator
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.services.fileserver.FileServer
import java.io.ByteArrayInputStream
import org.apache.http.HttpStatus
import org.springframework.util.FileCopyUtils
import java.io.FileOutputStream

class DownloadFileControllerTest extends TestBase with MockitoSugar {
	
	val controller = new DownloadFileController
	
	implicit val request = new MockHttpServletRequest
	implicit val response = new MockHttpServletResponse
	
	@Test
	def validFile() {
		val macGenerator = new SHAMessageAuthenticationCodeGenerator("someSalt")
		val fileDao = mock[FileDao]
		
		val attachment = new FileAttachment
		
		val file = createTemporaryFile
		FileCopyUtils.copy(new ByteArrayInputStream("yes".getBytes), new FileOutputStream(file))
		attachment.file = file
		
		when(fileDao.getFileById("abc")) thenReturn(Some(attachment))
		
		controller.macGenerator = macGenerator
		controller.fileDao = fileDao
		controller.fileServer = new FileServer
		
		controller.serve("abc", macGenerator.generateMessageAuthenticationCode("abc"))
		
		response.getStatus should be (HttpStatus.SC_OK)
		response.getContentAsString should be ("yes")
	}
	
	@Test
	def invalidMac() {
		val macGenerator = new SHAMessageAuthenticationCodeGenerator("someSalt")
		
		controller.macGenerator = macGenerator
		controller.serve("abc", "abc")
		
		response.getStatus should be (HttpStatus.SC_BAD_REQUEST)
	}
	
	@Test
	def invalidSecret() {
		val macGenerator = new SHAMessageAuthenticationCodeGenerator
		
		controller.macGenerator = macGenerator
		controller.serve("abc", "abc")
		
		response.getStatus should be (HttpStatus.SC_BAD_REQUEST)
	}

}