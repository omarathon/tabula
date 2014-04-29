package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import collection.JavaConversions._
import uk.ac.warwick.tabula.{Mockito, AppContextTestBase}
import java.util.zip.ZipInputStream
import java.io.{ByteArrayInputStream, FileInputStream}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.FileAttachment
import org.junit.Before
import uk.ac.warwick.tabula.coursework.commands.assignments.DownloadMarkersSubmissionsCommand
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.FileCopyUtils
import java.io.FileOutputStream
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue

class DownloadMarkerSubmissionTest extends AppContextTestBase with MarkingWorkflowWorld with Mockito {

  @Before
  def setup() {
    val attachment = new FileAttachment

    val file = createTemporaryFile()
    FileCopyUtils.copy(new ByteArrayInputStream("yes".getBytes), new FileOutputStream(file))

    attachment.file = file

    assignment.submissions.foreach {
      submission =>
        submission.values.add({
          val sv = new SavedFormValue()
          sv.attachments = Set(attachment)
          sv
        })
    }
  }

	trait CommandTestSupport extends AutowiringZipServiceComponent with AssignmentServiceComponent with StateServiceComponent {
		val assignmentService = mock[AssignmentService]
		val stateService = mock[StateService]
	}

  @Transactional
  @Test
  def downloadSubmissionsTest() {
    withUser("cuslaj") {
      val command = new DownloadMarkersSubmissionsCommand(assignment.module, assignment, currentUser) with CommandTestSupport
			command.callback = {
        zip =>
          val stream = new ZipInputStream(new FileInputStream(zip.file.get))
          val items = Zips.map(stream) {
            item => item.getName
          }
          items.size should be(3)
      }
			command.applyInternal()
    }
  }

}
