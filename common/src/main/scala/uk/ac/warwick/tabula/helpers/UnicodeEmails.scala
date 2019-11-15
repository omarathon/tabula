package uk.ac.warwick.tabula.helpers

import java.io.{ByteArrayInputStream, InputStream, OutputStream}

import javax.activation.DataSource
import javax.mail.internet.{MimeMessage, MimeUtility}
import org.springframework.mail.javamail.{MimeMessageHelper, MimeMessagePreparator}
import uk.ac.warwick.util.mail.WarwickMailSender

trait UnicodeEmails {

  def createMessage(sender: WarwickMailSender, multipart: Boolean)(fn: => MimeMessageHelper => Unit): MimeMessage = prepareMessage(sender.createMimeMessage, multipart)(fn)

  def prepareMessage(message: MimeMessage, multipart: Boolean)(fn: => MimeMessageHelper => Unit): MimeMessage = {
    val preparator = new FunctionalMimeMessagePreparator({ message =>
      val helper = new MimeMessageHelper(message, multipart, "UTF-8")
      fn(helper)
    })

    preparator.prepare(message)
    message.addHeader("X-Auto-Response-Suppress", "DR, OOF, AutoReply")

    message
  }

  def encodeSubject(subject: String): String = MimeUtility.encodeText(subject, "UTF-8", null)

}

object UnicodeEmails extends UnicodeEmails {
  case class ByteArrayDataSource(bytes: Array[Byte], contentType: String, fileName: String) extends DataSource {
    override def getInputStream: InputStream = new ByteArrayInputStream(bytes)

    override def getName: String = fileName

    override def getOutputStream: OutputStream = throw new UnsupportedOperationException("Read-only javax.activation.DataSource")

    override def getContentType: String = contentType
  }
}

class FunctionalMimeMessagePreparator(fn: => MimeMessage => Unit) extends MimeMessagePreparator {
  override def prepare(message: MimeMessage): Unit = fn(message)
}
