package uk.ac.warwick.tabula.data.model.notifications.coursework

import uk.ac.warwick.tabula.data.model.forms.SavedFormValue
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment, Notification, OriginalityReport}
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula.{Fixtures, TestBase}

class TurnitinJobSuccessNotificationTest extends TestBase with FreemarkerRendering {

	val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()

	val assignment: Assignment = Fixtures.assignment("5,000 word essay")
	assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
	assignment.id = "assignment-id"

	@Test def title(): Unit = withUser("cuscav", "0672089") {
		val report = new OriginalityReport

		val notification = Notification.init(new TurnitinJobSuccessNotification, currentUser.apparentUser, report, assignment)
		notification.title should be("CS118: The Turnitin check for \"5,000 word essay\" has finished")
	}

	@Test def allSuccessful(): Unit = withUser("cuscav", "0672089") {
		val report1 = new OriginalityReport
		val report2 = new OriginalityReport
		val report3 = new OriginalityReport

		report1.reportReceived = true
		report2.reportReceived = true
		report3.reportReceived = true

		val notification = Notification.init(new TurnitinJobSuccessNotification, currentUser.apparentUser, Seq(report1, report2, report3), assignment)
		val notificationContent = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
		notificationContent.trim should be(
			"""Plagiarism reports are ready for the 3 submissions for the assignment 'CS118 - 5,000 word essay'
				|you recently sent to Turnitin. Anyone with permission to view the assignment submissions can view the reports.""".stripMargin
		)
	}

	@Test def partialSuccess(): Unit = withUser("cuscav", "0672089") {
		val report1 = new OriginalityReport
		report1.lastTurnitinError = "This job failed previously, but we have since received the report"
		report1.reportReceived = true

		val report2 = new OriginalityReport
		report2.lastTurnitinError = "Failed to retrieve results"
		report2.attachment = new FileAttachment
		report2.attachment.name = "CS118 Essay.docx"
		report2.attachment.submissionValue = new SavedFormValue
		report2.attachment.submissionValue.submission = Fixtures.submission(universityId = "0000002")

		val report3 = new OriginalityReport
		report3.lastTurnitinError = "Failed to retrieve results"
		report3.attachment = new FileAttachment
		report3.attachment.name = "myessay.pdf"
		report3.attachment.submissionValue = new SavedFormValue
		report3.attachment.submissionValue.submission = Fixtures.submission(universityId = "0000003")

		val notification = Notification.init(new TurnitinJobSuccessNotification, currentUser.apparentUser, Seq(report1, report2, report3), assignment)
		val notificationContent = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
		notificationContent.trim should be(
			"""You recently sent 3 submissions for the assignment 'CS118 - 5,000 word essay' to Turnitin for a plagiarism check.
				|Turnitin checked 1 submission successfully but could not check the following 2 submissions:
				|
				|- 0000002 - CS118 Essay.docx - Failed to retrieve results
				|- 0000003 - myessay.pdf - Failed to retrieve results
				|
				|A plagiarism report for the submission is now ready.
				|Anyone with permission to view the assignment submissions can view the report.""".stripMargin
		)
	}

	@Test def allFailed(): Unit = withUser("cuscav", "0672089") {
		val report1 = new OriginalityReport
		report1.lastTurnitinError = "Failed to retrieve results"
		report1.attachment = new FileAttachment
		report1.attachment.name = "My special essay - 0000001.docx"
		report1.attachment.submissionValue = new SavedFormValue
		report1.attachment.submissionValue.submission = Fixtures.submission(universityId = "0000001")

		val report2 = new OriginalityReport
		report2.lastTurnitinError = "Failed to retrieve results"
		report2.attachment = new FileAttachment
		report2.attachment.name = "CS118 Essay.docx"
		report2.attachment.submissionValue = new SavedFormValue
		report2.attachment.submissionValue.submission = Fixtures.submission(universityId = "0000002")

		val report3 = new OriginalityReport
		report3.lastTurnitinError = "Failed to retrieve results"
		report3.attachment = new FileAttachment
		report3.attachment.name = "myessay.pdf"
		report3.attachment.submissionValue = new SavedFormValue
		report3.attachment.submissionValue.submission = Fixtures.submission(universityId = "0000003")

		val notification = Notification.init(new TurnitinJobSuccessNotification, currentUser.apparentUser, Seq(report1, report2, report3), assignment)
		val notificationContent = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
		notificationContent.trim should be(
			"""You recently sent 3 submissions for the assignment 'CS118 - 5,000 word essay' to Turnitin for a plagiarism check.
				|Turnitin could not check the following 3 submissions:
				|
				|- 0000001 - My special essay - 0000001.docx - Failed to retrieve results
				|- 0000002 - CS118 Essay.docx - Failed to retrieve results
				|- 0000003 - myessay.pdf - Failed to retrieve results""".stripMargin
		)
	}

}
