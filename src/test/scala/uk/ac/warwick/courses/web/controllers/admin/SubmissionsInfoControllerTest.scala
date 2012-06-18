package uk.ac.warwick.courses.web.controllers.admin

import collection.JavaConversions._
import uk.ac.warwick.courses.TestBase
import org.junit.Test
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import uk.ac.warwick.courses.commands.assignments.ListSubmissionsCommand
import uk.ac.warwick.courses.data.model.Submission
import uk.ac.warwick.courses.data.model.SavedSubmissionValue
import uk.ac.warwick.courses.data.model.Submission
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.FileAttachment
import uk.ac.warwick.courses.Mockito
import uk.ac.warwick.courses.services.SecurityService

class SubmissionsInfoControllerTest extends TestBase with Mockito {
	/**
	 * Check the dates are formatted correctly in the XML.
	 * The date format is defined in DateFormats so we're not really
	 * testing much about the controller itself here, just that it uses
	 * the correct thing.
	 */
	@Test def timeFormat = {
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
		controller.format(summerDate) should be ("2012-08-15T11:20:00+01:00")
		controller.format(winterDate) should be ("2012-11-15T11:20:00Z")
	}
	
	@Test def getXml {
		val controller = new SubmissionsInfoController()
		controller.securityService = mock[SecurityService]
		controller.checkIndex = false
		val command = new ListSubmissionsCommand
		val assignment = newDeepAssignment()
		command.assignment = assignment
		command.module = assignment.module
		command.assignment.submissions.addAll(Seq(
			submission(assignment, "0123456", Seq("Interesting helicopter.jpg"))
		))
		
		withUser("cusebr") {
			val result = controller.xml(command)
			(result\"submission"\"field"\"file"\"@zip-path").text should be ("IN101 - 0123456 - Interesting helicopter.jpg")
		}
	}
	
	def submission(assignment:Assignment, uniId:String, attachmentNames:Seq[String]) = {
		val s = new Submission
		s.assignment = assignment
		s.universityId = uniId
		if (!attachmentNames.isEmpty) {
			val attachments = (attachmentNames map toAttachment).toSet
			s.values.add(SavedSubmissionValue.withAttachments(s, Assignment.defaultUploadName, attachments ))
		}
		s
	}
	
	def toAttachment(attachmentName:String) = {
		val attachment = new FileAttachment
		attachment.name = attachmentName
		attachment
	}
}