package uk.ac.warwick.courses.commands

import scala.collection.mutable.ListBuffer
import scala.reflect.BeanProperty
import org.hibernate.annotations.AccessType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import javax.mail.internet.InternetAddress
import javax.persistence.Entity
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.helpers.StringUtils.StringToSuperString
import uk.ac.warwick.courses.helpers.FoundUser
import uk.ac.warwick.courses.helpers.NoUser
import uk.ac.warwick.courses.services.AssignmentService
import uk.ac.warwick.userlookup.User
import javax.mail.MessagingException

abstract class RecipientReportItem(val universityId:String, val user:User, val good:Boolean)
case class MissingUser(id:String) extends RecipientReportItem(id, null, false)
case class BadEmail(u:User) extends RecipientReportItem(u.getWarwickId, u, false)
case class GoodUser(u:User) extends RecipientReportItem(u.getWarwickId, u, true)

case class RecipientCheckReport(
	val users: List[RecipientReportItem]
)

/**
 * A standalone command to go through all the feedback for an assignment, looking up
 * all the students and reporting back on whether it looks like they have a working
 * email address. Used to show to the admin user which users may not receive an email
 * when feedback is published.
 */
@Configurable
class FeedbackRecipientCheckCommand extends Command[RecipientCheckReport] with Unaudited {
	
	@BeanProperty var assignment:Assignment =_
	@Autowired var assignmentService:AssignmentService =_
	
	override def apply = {
		val items:Seq[RecipientReportItem] = 
			for ((id, user) <- assignmentService.getUsersForFeedback(assignment)) 
				yield resolve(id,user)
		RecipientCheckReport(items.toList)
	}
	
	def resolve(id:String, user:User) = user match {
			case FoundUser(user) => {
				if (user.getEmail.hasText && isGoodEmail(user.getEmail)) {
					GoodUser(user)
				} else {
					BadEmail(user)
				}
			} 
			case NoUser(user) => MissingUser(id)
		}
		
	
	
	def isGoodEmail(email:String): Boolean = {
		try {
			new InternetAddress(email).validate
			true
		} catch {
			case e:MessagingException => false
		}
	}
}