package uk.ac.warwick.tabula.coursework.commands.assignments

import java.io.ByteArrayInputStream
import java.io.File
import org.joda.time.DateTime
import org.junit.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.coursework.TestBase
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.coursework.data.model.Assignment
import uk.ac.warwick.tabula.coursework.data.model.Submission
import uk.ac.warwick.tabula.coursework.data.model.FileAttachment
import uk.ac.warwick.tabula.coursework.data.FileDao
import uk.ac.warwick.tabula.coursework.data.model.forms.SubmissionValue
import uk.ac.warwick.tabula.coursework.data.model.SavedSubmissionValue
import uk.ac.warwick.tabula.JavaImports._
import java.util.HashSet
import uk.ac.warwick.tabula.coursework.data.model.forms.FileField
import uk.ac.warwick.tabula.coursework.data.model.forms.FileSubmissionValue
import uk.ac.warwick.tabula.coursework.commands.UploadedFile


class SubmitAssignmentCommandTest extends TestBase {

	@Autowired var dao: FileDao = _

	@Test def multipleSubmissions = withUser(code = "cusebr", universityId = "0678022") {
		val assignment = newActiveAssignment
		val user = RequestInfo.fromThread.get.user
		val cmd = new SubmitAssignmentCommand(assignment, user)

		var errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be(false)

		val submission = new Submission()
		submission.assignment = assignment
		submission.universityId = "0678022"
		assignment.submissions.add(submission)

		// Can't submit twice, silly
		errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be(true)

		// But guys, guys... what if...
		assignment.allowResubmission = true
		errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be(false)
	}

	@Test def fileTypeValidation = withUser(code = "cusebr", universityId = "0678022") {

		val user = RequestInfo.fromThread.get.user
		val assignment = newActiveAssignment
		val file = new FileField
		file.name = "upload"
		file.id = "upload"
		file.attachmentLimit = 4
		file.attachmentTypes = Seq("doc", "docx", "pdf")
		assignment.addField(file)
		

		// common reusable setup
		trait Setup {
			val cmd = new SubmitAssignmentCommand(assignment, user)
			var errors = new BindException(cmd, "command")
			val submissionValue = cmd.fields.get("upload").asInstanceOf[FileSubmissionValue]
		}

		new Setup {
			val document = resourceAsBytes("attachment1.docx")
			submissionValue.file.upload add new MockMultipartFile("attachment1.docx", "attachment1.docx", null, document)
			cmd.validate(errors)
			errors.hasErrors should be(false)
		}

		new Setup {
			val document2 = resourceAsBytes("attachment2.doc")
			submissionValue.file.upload add new MockMultipartFile("attachment2.doc", "attachment2.doc", null, document2)
			cmd.validate(errors)
			errors.hasErrors should be(false)
		}

		new Setup {
			val pdf = resourceAsBytes("attachment3.pdf")
			submissionValue.file.upload add new MockMultipartFile("attachment3.pdf", "attachment3.PDF", null, pdf)
			cmd.validate(errors)
			errors.hasErrors should be(false)
		}

		new Setup {
			val csv = resourceAsBytes("attachment4.csv")
			submissionValue.file.upload add new MockMultipartFile("attachment4.csv", "attachment4.csv", null, csv)
			cmd.validate(errors)
			errors.hasErrors should be(true)
			errors.getFieldError("fields[upload].file").getCode should be("file.wrongtype.one")
		}

		new Setup {
			val pdf = resourceAsBytes("attachment3.pdf")
			submissionValue.file.upload add new MockMultipartFile("attachment3.pdf", "attachment3.pdf", null, pdf)
			submissionValue.file.upload add new MockMultipartFile("attachment3.pdf", "attachment3.pdf", null, pdf)
			cmd.validate(errors)
			errors.hasErrors should be(true)
			errors.getFieldError("fields[upload].file").getCode should be("file.duplicate")
		}
	}

	def newActiveAssignment = {
		val assignment = new Assignment
		assignment.openDate = new DateTime().minusWeeks(1)
		assignment.closeDate = new DateTime().plusWeeks(1)
		assignment.collectSubmissions = true
		assignment.active = true
		assignment
	}
}