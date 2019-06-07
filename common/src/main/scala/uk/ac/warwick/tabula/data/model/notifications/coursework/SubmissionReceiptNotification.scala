package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.{SingleRecipientNotification, UniversityIdOrUserIdRecipientNotification}

@Entity
@Proxy(`lazy` = false)
@DiscriminatorValue("SubmissionReceipt")
class SubmissionReceiptNotification extends SubmissionNotification
  with SingleRecipientNotification
  with UniversityIdOrUserIdRecipientNotification {

  override def onPreSave(isNew: Boolean) {
    recipientUniversityId = submission.usercode
  }

  def title: String = "%s: Submission receipt for \"%s\"".format(moduleCode, assignment.name)

  @transient val templateLocation = "/WEB-INF/freemarker/emails/submissionreceipt.ftl"

  def urlTitle = "review your submission"

  def url: String = Routes.assignment(assignment)

}