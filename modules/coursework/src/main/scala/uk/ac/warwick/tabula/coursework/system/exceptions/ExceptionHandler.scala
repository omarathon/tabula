package uk.ac.warwick.tabula.coursework.system.exceptions

import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import scala.collection.JavaConversions.asScalaBuffer
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.InitializingBean
import org.springframework.mail.MailException
import org.springframework.mail.SimpleMailMessage
import freemarker.template.{ Configuration => FreemarkerConfiguration }
import freemarker.template.Template
import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.util.mail.WarwickMailSender
import uk.ac.warwick.tabula.coursework.web.views.FreemarkerRendering

case class ExceptionContext(val token: String, val exception: Throwable, val request: Option[HttpServletRequest] = None) {
	def getHasRequest = request.isDefined
}

trait ExceptionHandler {
	def exception(context: ExceptionContext)
}

class CompositeExceptionHandler(handlers: java.util.List[ExceptionHandler]) extends ExceptionHandler {
	private val _handlers = handlers.toList
	override def exception(context: ExceptionContext) =
		for (handler <- _handlers) handler.exception(context)
}

class LoggingExceptionHandler extends ExceptionHandler with Logging {
	override def exception(context: ExceptionContext) = context.exception match {
		case userError: UserError => if (debugEnabled) logger.debug("User error", userError)
		case handled: HandledException => if (debugEnabled) logger.debug("Handled exception", handled)
		case e => logger.error("Exception " + context.token, e)
	}
}

class EmailingExceptionHandler extends ExceptionHandler with Logging with InitializingBean with FreemarkerRendering {
	@Resource(name = "mailSender") var mailSender: WarwickMailSender = _
	@Value("${mail.exceptions.to}") var recipient: String = _
	@Autowired var freemarker: FreemarkerConfiguration = _
	var template: Template = _

	// Check for this exception without needing it on the classpath
	private val ClientAbortException = "org.apache.catalina.connector.ClientAbortException"

	override def exception(context: ExceptionContext) = context.exception match {
		case userError: UserError => {}
		case handled: HandledException => {}
		case e: IOException if e.getClass.getName equals ClientAbortException => {} // cancelled download.
		case e => {
			try {
				val message = makeEmail(context)
				mailSender.send(message)
			} catch {
				case e: MailException => logger.error("Error emailing exception " + context.token + "!", e)
			}
		}
	}

	def makeEmail(context: ExceptionContext) = {
		val info = RequestInfo.fromThread
		val message = new SimpleMailMessage
		message.setTo(recipient)
		message.setSubject("[HFCX] %s %s" format (userId(info), context.token))
		message.setText(renderToString(template, Map(
			"token" -> context.token,
			"exception" -> context.exception,
			"exceptionStack" -> renderStackTrace(context.exception),
			"requestInfo" -> info,
			"time" -> new DateTime,
			"request" -> context.request)))
		message
	}

	private def userId(info: Option[RequestInfo]) = info.map { _.user }.map { _.realId }.getOrElse("ANON")

	private def renderStackTrace(exception: Throwable) = {
		val stringWriter = new StringWriter
		val writer = new PrintWriter(stringWriter)
		exception.printStackTrace(writer)
		stringWriter.toString
	}

	override def afterPropertiesSet {
		template = freemarker.getTemplate("/WEB-INF/freemarker/emails/exception.ftl")
	}
}