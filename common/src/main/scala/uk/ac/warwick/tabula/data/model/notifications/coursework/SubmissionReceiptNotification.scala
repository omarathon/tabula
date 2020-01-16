package uk.ac.warwick.tabula.data.model.notifications.coursework

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}

import javax.activation.DataSource
import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import org.springframework.mail.javamail.MimeMessageHelper
import uk.ac.warwick.tabula.AutowiringTopLevelUrlComponent
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.profiles.PhotosWarwickMemberPhotoUrlGeneratorComponent
import uk.ac.warwick.tabula.data.model.{HasNotificationAttachment, SingleRecipientNotification, UniversityIdOrUserIdRecipientNotification}
import uk.ac.warwick.tabula.pdf.FreemarkerXHTMLPDFGeneratorComponent
import uk.ac.warwick.tabula.web.views.AutowiredTextRendererComponent

import scala.util.Using

@Entity
@Proxy
@DiscriminatorValue("SubmissionReceipt")
class SubmissionReceiptNotification extends SubmissionNotification
  with SingleRecipientNotification
  with UniversityIdOrUserIdRecipientNotification
  with HasNotificationAttachment {

  override def onPreSave(isNew: Boolean): Unit = {
    recipientUniversityId = submission.usercode
  }

  def title: String = "%s: Submission receipt for \"%s\"".format(moduleCode, assignment.name)

  @transient val templateLocation = "/WEB-INF/freemarker/emails/submissionreceipt.ftl"

  def urlTitle = "review your submission"

  def url: String = Routes.assignment(assignment)

  override def generateAttachments(message: MimeMessageHelper): Unit = {
    val dataSource: DataSource =
      new DataSource
        with FreemarkerXHTMLPDFGeneratorComponent
        with AutowiredTextRendererComponent
        with PhotosWarwickMemberPhotoUrlGeneratorComponent
        with AutowiringTopLevelUrlComponent {
        private lazy val receipt: Array[Byte] = Using.resource(new ByteArrayOutputStream) { baos =>
          pdfGenerator.renderTemplate(
            "/WEB-INF/freemarker/cm2/submit/submission-receipt.ftlh",
            Map("submission" -> submission),
            baos
          )
          baos.toByteArray
        }

        override def getInputStream: InputStream = new ByteArrayInputStream(receipt)
        override def getOutputStream: OutputStream = throw new UnsupportedOperationException("Read-only javax.activation.DataSource")
        override def getContentType: String = "application/pdf; charset=UTF-8"
        override def getName: String = "submission-receipt.pdf"
      }

    message.addAttachment("submission-receipt.pdf", dataSource)
  }
}
