package uk.ac.warwick.tabula.profiles.commands


import freemarker.template.Configuration
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.UnicodeEmails
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.util.concurrency.promise.Promise
import uk.ac.warwick.util.mail.WarwickMailSender
import scala.language.implicitConversions

class TutorChangeNotifierCommand(student: Member, oldTutor: Option[Member], newTutorPromise: Promise[Member])
	extends Command[Unit] with UnicodeEmails with Public with FreemarkerRendering {

	var notifyTutee: Boolean = false
	var notifyOldTutor: Boolean = false
	var notifyNewTutor: Boolean = false

	implicit var freemarker = Wire[Configuration]
	var mailSender = Wire[WarwickMailSender]("studentMailSender")
	var replyAddress = Wire[String]("${mail.noreply.to}")
	var fromAddress = Wire[String]("${mail.exceptions.to}")

	def applyInternal() {
		val newTutor = newTutorPromise.fulfilPromise

		if (notifyTutee) {
			logger.debug("Notifying tutee: " + student)
				mailSender.send(messageFor(
					"/WEB-INF/freemarker/emails/tutor_change_tutee_notification.ftl",
					student.email,
					student,
					oldTutor,
					newTutor,
					Routes.profile.view(student)))
		}
		if (notifyOldTutor && oldTutor.isDefined) {
			logger.debug("Notifying old tutor: " + oldTutor)
			mailSender.send(messageFor(
					"/WEB-INF/freemarker/emails/old_tutor_notification.ftl",
					oldTutor.get.email,
					student,
					oldTutor,
					newTutor,
					Routes.profile.view(student)))
		}
		if (notifyNewTutor) {
			logger.debug("Notifying new tutor: " + newTutor)
			mailSender.send(messageFor(
					"/WEB-INF/freemarker/emails/new_tutor_notification.ftl",
					newTutor.email,
					student,
					oldTutor,
					newTutor,
					Routes.profile.view(student)))
		}
	}

	def messageFor(template: String, toEmail: String, tutee: Member, oldTutor: Option[Member], newTutor: Member, path: String)
			= createMessage(mailSender) { message =>
		message.setFrom(fromAddress)
		message.setReplyTo(replyAddress)
		message.setTo(toEmail)
		message.setSubject(encodeSubject("Personal tutor change"))
		message.setText(renderToString(template, Map(
			"tutee" -> tutee,
			"oldTutor" -> oldTutor,
			"newTutor" -> newTutor,
			"path" -> path
		)))
	}

	override def describe(d: Description) = d.property("student ID" -> student.universityId).property("new tutor ID" -> newTutorPromise.fulfilPromise.universityId)
}
