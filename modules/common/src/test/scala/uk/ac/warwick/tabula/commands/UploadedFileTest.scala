package uk.ac.warwick.tabula.commands

import org.springframework.mock.web.MockMultipartFile
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.AppContextTestBase


class UploadedFileTest extends AppContextTestBase with Mockito{

	val multi1 = new MockMultipartFile("file", "feedback.doc", "text/plain", "aaaaaaaaaaaaaaaa".getBytes)
	val multiEmpty = new MockMultipartFile("file", null, "text/plain", null: Array[Byte])
	val multiUnderscorePrefix = new MockMultipartFile("file", "thumbs.db", "text/plain", "aaaaaaaaaaaa".getBytes)
	val multiSystemFile = new MockMultipartFile("file", "thumbs.db", "text/plain", "aaaaaaaaaaaa".getBytes)
	val multiAppleDouble = new MockMultipartFile("file", "._thing.doc", "text/plain", "aaaaaaaaaaaa".getBytes)
	
	@Test // HFC-375
	def ignoreEmptyMultipartFiles {
		val uploadedFile = new UploadedFile
		uploadedFile.fileDao = smartMock[FileDao]
		uploadedFile.upload = ArrayList(multi1, multiEmpty)
		uploadedFile.onBind
		
		uploadedFile.attached.size should be (1)
		uploadedFile.attached.get(0).name should be ("feedback.doc")
	}
	
	@Test
	def hasUploads {
		val uploadedFile = new UploadedFile
		uploadedFile.upload = ArrayList()
		uploadedFile.hasUploads should be (false)
		uploadedFile.uploadOrEmpty should be (ArrayList())
		
		uploadedFile.upload = ArrayList(multiEmpty)
		uploadedFile.hasUploads should be (false)
		uploadedFile.uploadOrEmpty should be (ArrayList())
		
		uploadedFile.upload = ArrayList(multi1)
		uploadedFile.hasUploads should be (true)
		uploadedFile.uploadOrEmpty should be (ArrayList(multi1))
	}


	@Test // TAB-48
	def ignoreSystemFiles {
		val uploadedFile = new UploadedFile
		uploadedFile.fileDao = smartMock[FileDao]
		uploadedFile.upload = ArrayList(multi1, multiSystemFile)
		uploadedFile.onBind
		
		uploadedFile.attached.size should be (1)
		uploadedFile.attached.get(0).name should be ("feedback.doc")
	}
	
	
	@Test // TAB-48
	def ignoreAppleDouble {
		val uploadedFile = new UploadedFile
		uploadedFile.fileDao = smartMock[FileDao]
		uploadedFile.upload = ArrayList(multi1, multiAppleDouble)
		uploadedFile.onBind
		
		uploadedFile.attached.size should be (1)
		uploadedFile.attached.get(0).name should be ("feedback.doc")
	}
		
	
}