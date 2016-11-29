package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.{SingleRecipientNotification, UniversityIdRecipientNotification}

@Entity
@DiscriminatorValue("SubmissionReceipt")
class SubmissionReceiptNotification extends SubmissionNotification
	with SingleRecipientNotification
	with UniversityIdRecipientNotification {

	override def onPreSave(isNew: Boolean) {
		recipientUniversityId = submission.universityId
	}

	def title: String = "%s: Submission receipt for \"%s\"".format(moduleCode, assignment.name)

	@transient val templateLocation = "/WEB-INF/freemarker/emails/submissionreceipt.ftl"

	def urlTitle = "review your submission"

	def url: String = Routes.assignment.receipt(assignment)

}