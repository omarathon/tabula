package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import java.io.{ByteArrayInputStream, FileInputStream, FileOutputStream}
import java.util.zip.ZipInputStream

import org.junit.Before
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.tabula.coursework.commands.assignments.DownloadMarkersSubmissionsCommand
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{Features, Mockito, TestBase}

import scala.collection.JavaConversions._

class DownloadMarkerSubmissionTest extends TestBase with MarkingWorkflowWorld with Mockito {

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
		assignment.markingWorkflow.userLookup = mockUserLookup
  }

	trait CommandTestSupport extends ZipServiceComponent with AssessmentServiceComponent with StateServiceComponent {
		val assessmentService = smartMock[AssessmentService]
		val stateService = smartMock[StateService]
		val zipService = new ZipService
		zipService.userLookup = mockUserLookup
		zipService.features = Features.empty
		zipService.zipDir = createTemporaryDirectory()
	}

  @Test
  def downloadSubmissionsTest() {
    withUser("cuslaj", "1111111") {
      val command = new DownloadMarkersSubmissionsCommand(assignment.module, assignment, currentUser.apparentUser, currentUser) with CommandTestSupport
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
