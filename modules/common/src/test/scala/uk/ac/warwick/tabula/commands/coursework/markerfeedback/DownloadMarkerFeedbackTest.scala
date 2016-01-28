package uk.ac.warwick.tabula.commands.coursework.markerfeedback

import java.io.{ByteArrayInputStream, FileInputStream, FileOutputStream}
import java.util.zip.ZipInputStream

import org.junit.Before
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.tabula.commands.coursework.feedback.{AdminGetSingleMarkerFeedbackCommand, DownloadMarkersFeedbackForPositionCommand}
import uk.ac.warwick.tabula.data.model.MarkingState._
import uk.ac.warwick.tabula.data.model.{FileAttachment, FirstFeedback}
import uk.ac.warwick.tabula.services.objectstore.ObjectStorageService
import uk.ac.warwick.tabula.services.{AutowiringZipServiceComponent, Zips}
import uk.ac.warwick.tabula.{Mockito, TestBase}

import scala.collection.JavaConversions._

class DownloadMarkerFeedbackTest extends TestBase with MarkingWorkflowWorld with Mockito {

	@Before
	def setup() {
		val attachment = new FileAttachment
		attachment.id = "123"

		val file = createTemporaryFile()
		FileCopyUtils.copy(new ByteArrayInputStream("yes".getBytes), new FileOutputStream(file))

		attachment.objectStorageService = smartMock[ObjectStorageService]
		attachment.objectStorageService.keyExists(attachment.id) returns true
		attachment.objectStorageService.metadata(attachment.id) returns Some(ObjectStorageService.Metadata(file.length(), "application/octet-stream", None))
		attachment.objectStorageService.fetch(attachment.id) answers { _ => Some(new FileInputStream(file)) }

		assignment.feedbacks.foreach{feedback =>
			feedback.firstMarkerFeedback.attachments = List(attachment)
			feedback.firstMarkerFeedback.state = MarkingCompleted
			val smFeedback = feedback.retrieveSecondMarkerFeedback
			smFeedback.state = ReleasedForMarking
		}
	}

	@Test
	def downloadSingle() { withUser("cuslaj"){
		val markerFeedback = assignment.getMarkerFeedback("9876004", currentUser.apparentUser, FirstFeedback)
		val command = new AdminGetSingleMarkerFeedbackCommand(assignment.module, assignment, markerFeedback.get)
		command.zipService = zipService
		val renderable = command.applyInternal()
		val stream = new ZipInputStream(new FileInputStream(renderable.file.get))
		val items = Zips.map(stream){item => item.getName}
		items.size should be (1)
	}}

	@Test
	def downloadAll() { withUser("cuslat", "1111111"){
		val command = new DownloadMarkersFeedbackForPositionCommand(assignment.module, assignment, currentUser.apparentUser, currentUser, FirstFeedback) with AutowiringZipServiceComponent
		assignment.markingWorkflow.userLookup = mockUserLookup
		command.zipService = zipService
		val renderable = command.applyInternal()
		val stream = new ZipInputStream(new FileInputStream(renderable.file.get))
		val items = Zips.map(stream){item => item.getName}
		items.size should be (3)
	}}

}
