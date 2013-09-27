package uk.ac.warwick.tabula.coursework.web.controllers.admin

import collection.JavaConversions._
import uk.ac.warwick.tabula.TestBase
import org.junit.Ignore
import org.joda.time.DateTime
import uk.ac.warwick.tabula.coursework.commands.assignments.ListSubmissionsCommand
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.services.SecurityService
import org.joda.time.DateTimeConstants

// scalastyle:off magic.number
class SubmissionsInfoControllerTest extends TestBase with Mockito {
	/**
	 * Check the dates are formatted correctly in the XML.
	 * The date format is defined in DateFormats so we're not really
	 * testing much about the controller itself here, just that it uses
	 * the correct thing.
	 */
	@Test def isoTimeFormat = {
		val controller = new SubmissionsInfoController()

		/**
		 * DST and non-DST dates are formatted with the relevant timezone
		 * on the end. Note that here, and in the app, we are implicitly
		 * relying on the system's timezone being correct when parsing a date
		 * that doesn't have a timezone in it.
		 *
		 * If we wanted to be all snazzy we could explicitly pass a timezone
		 * to DateTimeZone.setDefault() when starting the app.
		 */
		val summerDate = DateTime.parse("2012-08-15T11:20")
		val winterDate = DateTime.parse("2012-11-15T11:20")
		controller.isoFormat(summerDate) should be ("2012-08-15T11:20:00+01:00")
		controller.isoFormat(winterDate) should be ("2012-11-15T11:20:00Z")
	}

	@Test def csvTimeFormat = {
		val controller = new SubmissionsInfoController()

		/**
		 * For CSV, we don't specify timezone. Instead the format mirrors that
		 * used in Formsbuilder, which has default set in FormSubmission.java
		 */
		val validDate = DateTime.parse("2012-08-15T16:20")
		controller.csvFormat(validDate) should be ("15/08/2012 16:20")
	}

	@Test def getXml {
		val controller = new SubmissionsInfoController()
		controller.securityService = mock[SecurityService]
		controller.checkIndex = false

		val assignment = newDeepAssignment()
		val command = new ListSubmissionsCommand(assignment.module, assignment)

		val subDate = new DateTime(2012, DateTimeConstants.NOVEMBER, 27, 10, 44)
		command.assignment.submissions.addAll(Seq(
			submission(subDate, assignment, "0123456", Seq("Interesting helicopter.jpg"))
		))

		withUser("cusebr") {
			val result = controller.xml(command)
			(result\"submission"\"field"\"file"\"@zip-path").text should be ("IN101 - 0123456 - Interesting helicopter.jpg")
		}
	}

	@Ignore @Test def getCsv {
		val controller = new SubmissionsInfoController()
		controller.securityService = mock[SecurityService]
		controller.checkIndex = false

		val assignment = newDeepAssignment()
		val command = new ListSubmissionsCommand(assignment.module, assignment)

		val subDate = new DateTime(2012, DateTimeConstants.NOVEMBER, 27, 15, 44)
		assignment.id = "fakeassid"
		command.assignment.submissions.addAll(Seq(
			submission(subDate, assignment, "0123456", Seq("Interesting helicopter.jpg"))
		))

		withUser("cusxad") {
			val actual = controller.csv(command).getAsString
			val expected = """	|"submission-id","submission-time","university-id","assignment-id","downloaded","upload-name","upload-zip-path"
								|"fakesubid","27/11/2012 15:44","0123456","fakeassid","false","Interesting helicopter.jpg","IN101 - 0123456 - Interesting helicopter.jpg"
								|""".stripMargin

			actual should be (expected)
		}
	}

	def submission(submittedDate: DateTime, assignment:Assignment, uniId:String, attachmentNames:Seq[String]) = {
		val s = new Submission
		s.assignment = assignment
		s.universityId = uniId
		s.submittedDate = submittedDate
		s.id = "fakesubid"
		if (!attachmentNames.isEmpty) {
			val attachments = (attachmentNames map toAttachment).toSet
			s.values.add(SavedFormValue.withAttachments(s, Assignment.defaultUploadName, attachments ))
		}
		s
	}

	def toAttachment(attachmentName:String) = {
		val attachment = new FileAttachment
		attachment.name = attachmentName
		attachment
	}
}