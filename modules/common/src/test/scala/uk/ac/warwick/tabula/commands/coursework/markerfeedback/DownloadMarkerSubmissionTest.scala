package uk.ac.warwick.tabula.commands.coursework.markerfeedback

import java.io.{ByteArrayInputStream, FileInputStream, FileOutputStream}
import java.util.zip.ZipInputStream

import org.junit.Before
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.tabula.commands.coursework.assignments.DownloadMarkersSubmissionsCommand
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.objectstore.ObjectStorageService
import uk.ac.warwick.tabula.{Features, Mockito, TestBase}

import scala.collection.JavaConversions._

class DownloadMarkerSubmissionTest extends TestBase with MarkingWorkflowWorld with Mockito {

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
			val zip = command.applyInternal()
      val stream = new ZipInputStream(new FileInputStream(zip.file.get))
      val items = Zips.map(stream) {
        item => item.getName
      }
      items.size should be(3)
    }
  }

}
