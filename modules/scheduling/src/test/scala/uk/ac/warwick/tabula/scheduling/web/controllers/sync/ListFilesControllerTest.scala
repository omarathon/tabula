package uk.ac.warwick.tabula.scheduling.web.controllers.sync

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.FileDao
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import uk.ac.warwick.tabula.web.views.JSONView
import org.json.JSONObject
import uk.ac.warwick.tabula.data.model.FileAttachment

// scalastyle:off magic.number
class ListFilesControllerTest extends TestBase with MockitoSugar {
	
	val controller = new ListFilesController
	
	@Test(expected=classOf[IllegalArgumentException])
	def noStartParam() {
		controller.list(-1, null)
		
		fail("expected an exception")
	}
	
	@Test
	def emptyList() {
		val start = 123
		val startDate = new DateTime(start)
		
		val dao = mock[FileDao]
		when(dao.getFilesCreatedSince(startDate, ListFilesController.MaxResponses)) thenReturn Seq()
		
		controller.fileDao = dao
		
		val json = controller.list(start, null).view.asInstanceOf[JSONView].json.asInstanceOf[Map[String, Any]]
		json.get("createdSince") should be (Some(123L))
		json.get("maxResponses") should be (Some(ListFilesController.MaxResponses))
		json.get("lastFileReceived") should be (Some(null))
		json.get("files") should be (Some(Seq()))
	}
	
	@Test
	def filesCreatedSince() {
		val start = 123
		val startDate = new DateTime(start)
		
		val dao = mock[FileDao]
		
		val now = new DateTime()
		val yesterday = now.minusDays(1)
		
		val attachment1 = new FileAttachment
		attachment1.id = "1"
		attachment1.dateUploaded = now
		
		val attachment2 = new FileAttachment
		attachment2.id = "2"
		attachment2.dateUploaded = yesterday
		
		when(dao.getFilesCreatedSince(startDate, ListFilesController.MaxResponses)) thenReturn Seq(attachment1, attachment2)
		
		controller.fileDao = dao
		
		val json = controller.list(start, null).view.asInstanceOf[JSONView].json.asInstanceOf[Map[String, Any]]
		json.get("createdSince") should be (Some(123L))
		json.get("maxResponses") should be (Some(ListFilesController.MaxResponses))
		json.get("lastFileReceived") should be (Some(attachment2.dateUploaded.getMillis))
		
		val files = json.get("files").get.asInstanceOf[Seq[Map[String, Any]]]
		files.length should be (2)
		
		files.head.get("id") should be (Some("1"))
		files.head.get("createdDate") should be (Some(attachment1.dateUploaded.getMillis))
		
		files.tail.head.get("id") should be (Some("2"))
		files.tail.head.get("createdDate") should be (Some(attachment2.dateUploaded.getMillis))
	}
	
	@Test
	def filesCreatedOn() {
		val start = 123
		val startDate = new DateTime(start)
		
		val dao = mock[FileDao]
		
		val now = new DateTime()
		val yesterday = now.minusDays(1)
		
		val attachment1 = new FileAttachment
		attachment1.id = "1"
		attachment1.dateUploaded = now
		
		val attachment2 = new FileAttachment
		attachment2.id = "2"
		attachment2.dateUploaded = yesterday
		
		when(dao.getFilesCreatedOn(startDate, ListFilesController.MaxResponses, "startingIdString")) thenReturn Seq(attachment1, attachment2)
		
		controller.fileDao = dao
		
		val json = controller.list(start, "startingIdString").view.asInstanceOf[JSONView].json.asInstanceOf[Map[String, Any]]
		json.get("createdSince") should be (Some(123L))
		json.get("maxResponses") should be (Some(ListFilesController.MaxResponses))
		json.get("lastFileReceived") should be (Some(attachment2.dateUploaded.getMillis))
		
		val files = json.get("files").get.asInstanceOf[Seq[Map[String, Any]]]
		files.length should be (2)
		
		files.head.get("id") should be (Some("1"))
		files.head.get("createdDate") should be (Some(attachment1.dateUploaded.getMillis))
		
		files.tail.head.get("id") should be (Some("2"))
		files.tail.head.get("createdDate") should be (Some(attachment2.dateUploaded.getMillis))
	}

}