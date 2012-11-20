package uk.ac.warwick.tabula.home.commands

import java.lang.Boolean
import java.util.concurrent.Future
import scala.annotation.target.field
import scala.reflect.BeanProperty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.InitializingBean
import org.springframework.mail.SimpleMailMessage
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils
import freemarker.template.Configuration
import freemarker.template.Template
import javax.annotation.Resource
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.util.mail.WarwickMailSender
import uk.ac.warwick.util.core.StringUtils._
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands._

class AppCommentCommand(user: CurrentUser) extends Command[Future[Boolean]] with FreemarkerRendering with InitializingBean {

	var mailSender = Wire[WarwickMailSender]("mailSender")
	var adminMailAddress = Wire.property("${mail.admin.to}")
	var freemarker = Wire.auto[Configuration]
	
	var template: Template = _

	@BeanProperty var message: String = _
	//	@BeanProperty var pleaseRespond:Boolean =_
	@BeanProperty var usercode: String = _
	@BeanProperty var name: String = _
	@BeanProperty var email: String = _
	@BeanProperty var currentPage: String = _
	@BeanProperty var browser: String = _
	@BeanProperty var os: String = _
	@BeanProperty var resolution: String = _
	@BeanProperty var ipAddress: String = _

	def work = {
		val mail = new SimpleMailMessage
		mail setTo adminMailAddress
		mail setFrom adminMailAddress
		mail setSubject "Tabula feedback"
		mail setText generateText

		mailSender send mail
	}

	def prefill = {
		if (user != null && user.loggedIn) {
			if (!hasText(usercode)) usercode = user.apparentId
			if (!hasText(name)) name = user.fullName
			if (!hasText(email)) email = user.email
		}
	}

	def generateText = renderToString(template, Map(
		"user" -> user,
		"info" -> this))

	def afterPropertiesSet {
		template = freemarker.getTemplate("/WEB-INF/freemarker/emails/appfeedback.ftl")
	}

	def validate(errors: Errors) {
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "message", "NotEmpty")
	}

	def describe(d: Description) {}

	override def describeResult(d: Description) = d.properties(
		"name" -> name,
		"email" -> email,
		"message" -> message)

}