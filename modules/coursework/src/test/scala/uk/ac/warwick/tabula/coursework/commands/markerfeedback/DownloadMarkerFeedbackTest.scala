package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import collection.JavaConversions._
import uk.ac.warwick.tabula.{Mockito, AppContextTestBase}
import org.junit.Before
import uk.ac.warwick.tabula.data.model.MarkingState._
import uk.ac.warwick.tabula.data.model.FileAttachment
import org.mockito.Mockito._
import java.io.{FileInputStream, ByteArrayInputStream}
import uk.ac.warwick.tabula.coursework.commands.feedback.{DownloadFirstMarkersFeedbackCommand, AdminGetSingleMarkerFeedbackCommand}
import java.util.zip.ZipInputStream
import uk.ac.warwick.tabula.services.Zips

class DownloadMarkerFeedbackTest extends AppContextTestBase with Mockito with MarkingWorkflowWorld {

	@Before
	def setup {
		val attachment = mock[FileAttachment]
		when(attachment.length) thenReturn(None)
		when(attachment.dataStream) thenReturn(new ByteArrayInputStream("yes".getBytes))

		assignment.feedbacks.foreach{feedback =>
			feedback.firstMarkerFeedback.attachments = List(attachment)
			feedback.firstMarkerFeedback.state = MarkingCompleted
			val smFeedback = feedback.retrieveSecondMarkerFeedback
			smFeedback.state = ReleasedForMarking
		}
	}

	@Test
	def downloadSingle(){
		withUser("cuslaj"){
			val markerFeedback = assignment.getMarkerFeedback("9876004", currentUser.apparentUser)
			val command = new AdminGetSingleMarkerFeedbackCommand(assignment.module, assignment, markerFeedback.get)
			val renderable = command.apply()
			val stream = new ZipInputStream(new FileInputStream(renderable.file.get))
			val items = Zips.map(stream){item => item.getName}
			items.size should be (1)
		}
	}

	@Test
	def downloadAll(){
		withUser("cuslat"){
			val command = new DownloadFirstMarkersFeedbackCommand(assignment.module, assignment, currentUser)
			val renderable = command.apply()
			val stream = new ZipInputStream(new FileInputStream(renderable.file.get))
			val items = Zips.map(stream){item => item.getName}
			items.size should be (3)
		}
	}

}
