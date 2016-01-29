package uk.ac.warwick.tabula.commands.coursework.markerfeedback

import java.io.{ByteArrayInputStream, FileInputStream}
import java.util.zip.ZipInputStream

import org.junit.Before
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

		attachment.objectStorageService = zipService.objectStorageService
		attachment.objectStorageService.push(attachment.id, new ByteArrayInputStream("yes".getBytes), ObjectStorageService.Metadata(3, "application/octet-stream", None))

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
		val stream = new ZipInputStream(renderable.inputStream)
		val items = Zips.map(stream){item => item.getName}
		items.size should be (1)
	}}

	@Test
	def downloadAll() { withUser("cuslat", "1111111"){
		val command = new DownloadMarkersFeedbackForPositionCommand(assignment.module, assignment, currentUser.apparentUser, currentUser, FirstFeedback) with AutowiringZipServiceComponent
		assignment.markingWorkflow.userLookup = mockUserLookup
		command.zipService = zipService
		val renderable = command.applyInternal()
		val stream = new ZipInputStream(renderable.inputStream)
		val items = Zips.map(stream){item => item.getName}
		items.size should be (3)
	}}

}
