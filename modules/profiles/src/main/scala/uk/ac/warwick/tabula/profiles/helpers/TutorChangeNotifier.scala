package uk.ac.warwick.tabula.profiles.helpers

import freemarker.template.Configuration
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.UnicodeEmails
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.util.mail.WarwickMailSender

class TutorChangeNotifier(student: Member, oldTutor: Member, notifyTutee: Boolean, notifyOldTutor: Boolean, notifyNewTutor: Boolean)
		extends UnicodeEmails with Public with FreemarkerRendering {

	implicit var freemarker = Wire.auto[Configuration]
	val mailSender = Wire[WarwickMailSender]("studentMailSender")
	var profileService = Wire.auto[ProfileService]
	val replyAddress = Wire.property("${mail.noreply.to}")
	val fromAddress = Wire.property("${mail.exceptions.to}")
	var memberDao = Wire.auto[MemberDao]
	
	val newTutor = profileService.getPersonalTutor(student).getOrElse(throw new IllegalStateException("Couldn't find database information for new tutor for student " + student.universityId))

	logger.debug("Old tutor: " + oldTutor)
	logger.debug("New tutor: " + newTutor)
	logger.debug("Student: " + student)

	def sendNotifications() = {
		if (notifyTutee) {
			logger.debug("Notifying tutee: " + student)			
			mailSender send messageFor(
					"/WEB-INF/freemarker/emails/tutor_change_tutee_notification.ftl", 
					student.email, 
					student, 
					oldTutor, 
					newTutor, 
					Routes.profile.view(student))
		}
		if (notifyOldTutor) {
			logger.debug("Notifying old tutor: " + oldTutor)
			mailSender send messageFor(
					"/WEB-INF/freemarker/emails/old_tutor_notification.ftl", 
					oldTutor.email, 
					student, 
					oldTutor, 
					newTutor, 
					Routes.profile.view(student))
		}
		if (notifyNewTutor) {
			logger.debug("Notifying new tutor: " + newTutor)
			mailSender send messageFor(
					"/WEB-INF/freemarker/emails/new_tutor_notification.ftl", 
					newTutor.email, 
					student, 
					oldTutor, 
					newTutor,
					Routes.profile.view(student))
		}
	}

	def messageFor(template: String, toEmail: String, tutee: Member, oldTutor: Member, newTutor: Member, path: String) 
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
}
