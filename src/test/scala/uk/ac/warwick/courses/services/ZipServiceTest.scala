package uk.ac.warwick.courses.services
import uk.ac.warwick.courses.TestBase
import org.junit.Test
import java.util.zip.ZipInputStream
import org.springframework.core.io.ClassPathResource
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.courses.data.model.Submission
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.data.model.Assignment
import collection.JavaConversions._
import uk.ac.warwick.courses.data.model.forms.FileSubmissionValue
import uk.ac.warwick.courses.data.model.SavedSubmissionValue
import uk.ac.warwick.courses.data.model.FileAttachment

class ZipServiceTest extends TestBase {
	
	@Test def generateSubmissionDownload {
		val service = new ZipService
		service.zipDir = createTemporaryDirectory
		
		val module = new Module(code="ph105")
		val assignment = new Assignment
		val submission = new Submission
		submission.universityId = "0678888"
		submission.assignment = assignment
		
		val attachment = new FileAttachment
		attachment.name = "garble.doc"
		attachment.file = createTemporaryFile
		
		submission.values = Set(SavedSubmissionValue.withAttachments(submission, "files", Set(attachment)))
		assignment.module = module
		val items = service.getSubmissionZipItems(submission)
		items.size should be (1)
		items.head.name should be ("ph105 - 0678888 - garble.doc")
	}
	
	@Test def readZip {
		val zip = new ZipInputStream(new ClassPathResource("/feedback1.zip").getInputStream)
		val names = Zips.map(zip){ _.getName }.sorted
		names should have ('size(8))
		names should contain("0123456/")
		names should contain("0123456/feedback.doc")
		names should contain("0123456/feedback.mp3")
		names should contain("0123456/feedback.txt")
		names should contain("0123457/")
		names should contain("0123457/crayons.doc")
		names should contain("0123457/feedback.mp3")
		names should contain("marks.csv")
	}
	
	@Test def iterateZip {
		val zip = new ZipInputStream(new ClassPathResource("/feedback1.zip").getInputStream)
		val names = Zips.iterator(zip){ (iterator) =>
			for (i <- iterator) yield i.getName
		}
		names should have ('size(8))
		names should contain("0123456/")
		names should contain("0123456/feedback.doc")
		names should contain("0123456/feedback.mp3")
		names should contain("0123456/feedback.txt")
		names should contain("0123457/")
		names should contain("0123457/crayons.doc")
		names should contain("0123457/feedback.mp3")
		names should contain("marks.csv")
	}
}