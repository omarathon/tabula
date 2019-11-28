package uk.ac.warwick.tabula.notifications

import javax.mail.Message.RecipientType
import javax.mail.Session
import javax.mail.internet.{InternetAddress, MimeMessage, MimeMultipart}
import org.hamcrest.{BaseMatcher, Description}
import uk.ac.warwick.tabula.data.model.notifications.profiles.StudentRelationshipChangeToStudentNotification
import uk.ac.warwick.tabula.data.model.{Notification, StudentRelationship}
import uk.ac.warwick.tabula.profiles.TutorFixture
import uk.ac.warwick.tabula.services.NotificationService
import uk.ac.warwick.tabula.web.views.{FreemarkerTextRenderer, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.util.concurrency.ImmediateFuture
import uk.ac.warwick.util.mail.WarwickMailSender

class EmailNotificationListenerTest extends TestBase with Mockito with TutorFixture {

  val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()
  val listener: EmailNotificationListener = new EmailNotificationListener {
    override def textRenderer: FreemarkerTextRenderer = new FreemarkerTextRenderer {
      override def renderTemplate(templateId: String, model: Any): String =
        renderToString(templateId, model)(freeMarkerConfig)
    }

    topLevelUrl = "https://tabu.la"
    mailSender = smartMock[WarwickMailSender]
    mailSender.createMimeMessage() answers { _: Array[AnyRef] => new MimeMessage(Session.getDefaultInstance(System.getProperties)) }
    mailSender.send(any[MimeMessage]) returns ImmediateFuture.of(true)
    service = smartMock[NotificationService]
    replyAddress = "no-reply@tabu.la"
    fromAddress = "admin@tabu.la"
  }

  @Test def studentRelationshipChangeToStudentNotification(): Unit = {
    // The agent isn't important here
    val notification = Notification.init(new StudentRelationshipChangeToStudentNotification, actor, relationship: StudentRelationship)
    notification.preSave(true)
    notification.recipientNotificationInfos.size() should be (1)
    notification.title should be("Personal tutor allocation change")

    listener.listen(notification.recipientNotificationInfos.get(0))

    verify(listener.mailSender, times(1)).send(anArgThat(new BaseMatcher[MimeMessage] {
      override def matches(actual: Any): Boolean = actual match {
        case message: MimeMessage =>
          InternetAddress.toString(message.getFrom) should be ("admin@tabu.la")
          InternetAddress.toString(message.getReplyTo) should be ("no-reply@tabu.la")
          InternetAddress.toString(message.getRecipients(RecipientType.TO)) should be ("student@warwick.ac.uk")

          val mixedMultipart = message.getContent.asInstanceOf[MimeMultipart]
          mixedMultipart.getContentType should startWith ("multipart/mixed")
          mixedMultipart.getCount should be (1)

          val relatedMultipart = mixedMultipart.getBodyPart(0).getContent.asInstanceOf[MimeMultipart]
          relatedMultipart.getContentType should startWith ("multipart/related")
          relatedMultipart.getCount should be (1)

          val multipart = relatedMultipart.getBodyPart(0).getContent.asInstanceOf[MimeMultipart]
          multipart.getContentType should startWith ("multipart/alternative")
          multipart.getCount should be (2)

          val textPart = multipart.getBodyPart(0)
          textPart.getDataHandler.getContentType should be ("text/plain; charset=UTF-8")
          textPart.getDataHandler.getContent.asInstanceOf[String] should include ("You have been assigned Peter O'Hanraha-Hanrahan as your personal tutor.")

          val htmlPart = multipart.getBodyPart(1)
          htmlPart.getDataHandler.getContentType should be ("text/html;charset=UTF-8")
          htmlPart.getDataHandler.getContent.asInstanceOf[String] should include ("<p>You have been assigned Peter O&#39;Hanraha-Hanrahan as your personal tutor.</p>")

          message.getHeader("X-Auto-Response-Suppress") should be (Array("DR, OOF, AutoReply"))

          true
        case _ => false
      }
      override def describeTo(description: Description): Unit = description.appendText("an email with the correct text")
    }))
  }

}
