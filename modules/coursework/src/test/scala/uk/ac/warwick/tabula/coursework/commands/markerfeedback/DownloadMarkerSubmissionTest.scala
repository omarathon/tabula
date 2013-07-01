package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import collection.JavaConversions._
import uk.ac.warwick.tabula.{Mockito, AppContextTestBase}
import java.util.zip.ZipInputStream
import java.io.{ByteArrayInputStream, FileInputStream}
import uk.ac.warwick.tabula.services.{StateService, Zips}
import uk.ac.warwick.tabula.data.model.{SavedSubmissionValue, FileAttachment}
import org.junit.Before
import uk.ac.warwick.tabula.coursework.commands.assignments.DownloadMarkersSubmissionsCommand
import org.apache.velocity.tools.config.SkipSetters
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.FileCopyUtils
import java.io.FileOutputStream


class DownloadMarkerSubmissionTest extends AppContextTestBase with MarkingWorkflowWorld with Mockito {

  @Before
  def setup {
    val attachment = new FileAttachment

    val file = createTemporaryFile
    FileCopyUtils.copy(new ByteArrayInputStream("yes".getBytes), new FileOutputStream(file))

    attachment.file = file

    assignment.submissions.foreach {
      submission =>
        submission.values.add({
          val sv = new SavedSubmissionValue()
          sv.attachments = Set(attachment)
          sv
        })
    }
  }

  @Transactional
  @Test
  def downloadSubmissionsTest {
    withUser("cuslaj") {
      val command = new DownloadMarkersSubmissionsCommand(assignment.module, assignment, currentUser)
      command.stateService = mock[StateService]
      command.apply {
        zip =>
          val stream = new ZipInputStream(new FileInputStream(zip.file.get))
          val items = Zips.map(stream) {
            item => item.getName
          }
          items.size should be(3)
      }
    }
  }

}
