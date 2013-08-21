package uk.ac.warwick.tabula.profiles.notifications

import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.profiles.commands.tutor.PersonalTutorChange
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.SingleRecipientNotification
import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.profiles.web.Routes

abstract class BulkTutorChangeNotification (
	private val changes: Seq[PersonalTutorChange],
	val agent: User,
	val recipient: User,
	private val templateLocation: String
) extends Notification[Seq[PersonalTutorChange]] with SingleRecipientNotification {
	
	this: TextRenderer =>
		
	private val tutees = changes.flatMap(_.modifiedRelationship.studentMember)
		
	val verb: String = "change"
	val _object = changes
	val target: Option[AnyRef] = Some(tutees)
	
	def title: String = "Personal tutor change"
	def content: String = {
		renderTemplate(templateLocation, Map(
			"changes" -> changes,
			"path" -> url
		) ++ extraModel)
	}
	
	def extraModel: Map[String, Any]
	def url: String

}

class BulkTuteeNotification (
	change: PersonalTutorChange, 
	agent: User, 
	recipient: User,
	private val tutee: Member
) extends BulkTutorChangeNotification(Seq(change), agent, recipient, BulkPersonalTutorChangeNotification.TuteeTemplate) {
	
	this: TextRenderer =>
		
	val newTutor = 
		if (change.modifiedRelationship.endDate != null && change.modifiedRelationship.endDate.isBeforeNow) None
		else change.modifiedRelationship.agentMember
		
	def extraModel = Map(
		"tutee" -> tutee,
		"oldTutor" -> change.oldTutor,
		"newTutor" -> newTutor
	)
		
	def url: String = Routes.profile.view(tutee)
	
}

class BulkNewTutorNotification (
	changes: Seq[PersonalTutorChange],
	agent: User, 
	recipient: User,
	private val newTutor: Member
) extends BulkTutorChangeNotification(changes, agent, recipient, BulkPersonalTutorChangeNotification.NewTutorTemplate) {
	
	this: TextRenderer =>
		
	def extraModel = Map(
		"newTutor" -> newTutor
	)
		
	def url: String = Routes.tutees
	
}

class BulkOldTutorNotification (
	changes: Seq[PersonalTutorChange],
	agent: User, 
	recipient: User,
	private val oldTutor: Member
) extends BulkTutorChangeNotification(changes, agent, recipient, BulkPersonalTutorChangeNotification.OldTutorTemplate) {
	
	this: TextRenderer =>
		
	def extraModel = Map(
		"oldTutor" -> oldTutor
	)
		
	def url: String = Routes.tutees
	
}

object BulkPersonalTutorChangeNotification {
	val NewTutorTemplate = "/WEB-INF/freemarker/notifications/bulk_new_tutor_notification.ftl"
	val OldTutorTemplate = "/WEB-INF/freemarker/notifications/bulk_old_tutor_notification.ftl"
	val TuteeTemplate = "/WEB-INF/freemarker/notifications/tutor_change_tutee_notification.ftl"
}