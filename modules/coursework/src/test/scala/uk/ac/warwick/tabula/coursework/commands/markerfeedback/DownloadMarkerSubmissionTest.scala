package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import collection.JavaConversions._
import uk.ac.warwick.tabula.{Mockito, AppContextTestBase}
import java.util.zip.ZipInputStream
import java.io.{ByteArrayInputStream, FileInputStream}
import uk.ac.warwick.tabula.services.Zips
import uk.ac.warwick.tabula.data.model.{SavedSubmissionValue, FileAttachment}
import org.mockito.Mockito._
import org.junit.Before
import uk.ac.warwick.tabula.coursework.commands.assignments.DownloadMarkersSubmissionsCommand
import org.apache.velocity.tools.config.SkipSetters
import org.springframework.transaction.annotation.Transactional


class DownloadMarkerSubmissionTest extends AppContextTestBase with Mockito with MarkingWorkflowWorld {

	@Before
	def setup {
		val attachment = mock[FileAttachment]
		when(attachment.length) thenReturn(None)
		when(attachment.dataStream) thenReturn(new ByteArrayInputStream("yes".getBytes))
		assignment.submissions.foreach{submission =>
			submission.values.add({
				val sv = new SavedSubmissionValue()
				sv.attachments = Set(attachment)
				sv
			})
		}
	}

	@Transactional @Test
	def downloadSubmissionsTest {
		withUser("cuslaj"){
			val command = new DownloadMarkersSubmissionsCommand(assignment.module, assignment, currentUser)
			command.apply { zip =>
				val stream = new ZipInputStream(new FileInputStream(zip.file.get))
				val items = Zips.map(stream){item => item.getName}
				items.size should be (3)
			}
		}
	}

}
