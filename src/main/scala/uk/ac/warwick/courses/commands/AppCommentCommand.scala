package uk.ac.warwick.courses.commands

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
import uk.ac.warwick.courses.helpers.FreemarkerRendering
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.util.mail.WarwickMailSender

@Configurable
class AppCommentCommand(user:CurrentUser) extends Command[Future[Boolean]] with FreemarkerRendering with InitializingBean {

	@Resource(name="mailSender")
	@BeanProperty var mailSender:WarwickMailSender =_
	
	@Value("${mail.admin.to}") var adminMailAddress:String =_
	
	@Autowired
	@BeanProperty var freemarker:Configuration =_
	var template:Template = _
	
	@BeanProperty var message:String =_
	
	@BeanProperty var pleaseRespond:Boolean =_
	
	def apply = {
		val mail = new SimpleMailMessage
		mail setTo adminMailAddress
		mail setFrom adminMailAddress
		mail setSubject "Coursework application feedback"
		mail setText generateText
		
		mailSender send mail
	}
	
	def generateText = renderToString(template, Map(
			"message" -> message,
			"pleaseRespond" -> pleaseRespond,
			"user" -> user
		))
	
	def afterPropertiesSet {
		template = freemarker.getTemplate("/WEB-INF/freemarker/emails/appfeedback.ftl")
	}
	
	def validate(errors:Errors) {
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "message", "NotEmpty")
	}
	
	def describe(d:Description) {}
	
	override def describeResult(d:Description) {}
	
}