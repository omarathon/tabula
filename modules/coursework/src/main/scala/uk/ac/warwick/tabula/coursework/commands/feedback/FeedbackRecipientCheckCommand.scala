package uk.ac.warwick.tabula.coursework.commands.feedback
import scala.reflect.BeanProperty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import javax.mail.internet.InternetAddress
import uk.ac.warwick.tabula.coursework.data.model.Assignment
import uk.ac.warwick.tabula.helpers.StringUtils.StringToSuperString
import uk.ac.warwick.tabula.helpers.FoundUser
import uk.ac.warwick.tabula.helpers.NoUser
import uk.ac.warwick.tabula.coursework.services.AssignmentService
import uk.ac.warwick.userlookup.User
import javax.mail.MessagingException
import uk.ac.warwick.tabula.coursework.data.model.Module
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import scala.reflect.BeanProperty
import uk.ac.warwick.tabula.coursework.commands.Command
import uk.ac.warwick.tabula.coursework.commands.ReadOnly
import uk.ac.warwick.tabula.coursework.commands.Unaudited
import uk.ac.warwick.spring.Wire

abstract class RecipientReportItem(val universityId: String, val user: User, val good: Boolean)
case class MissingUser(id: String) extends RecipientReportItem(id, null, false)
case class BadEmail(u: User) extends RecipientReportItem(u.getWarwickId, u, false)
case class GoodUser(u: User) extends RecipientReportItem(u.getWarwickId, u, true)

case class RecipientCheckReport(
	val users: List[RecipientReportItem]) {
	def hasProblems = users.find { !_.good }.isDefined
	def problems = users.filter { !_.good }
}

/**
 * A standalone command to go through all the feedback for an assignment, looking up
 * all the students and reporting back on whether it looks like they have a working
 * email address. Used to show to the admin user which users may not receive an email
 * when feedback is published.
 */
class FeedbackRecipientCheckCommand extends Command[RecipientCheckReport] with Unaudited with ReadOnly {

	var assignmentService = Wire.auto[AssignmentService]

	@BeanProperty var module: Module = _ // optional, mainly for binding from URL
	@BeanProperty var assignment: Assignment = _

	override def work = {
		val items: Seq[RecipientReportItem] =
			for ((id, user) <- assignmentService.getUsersForFeedback(assignment))
				yield resolve(id, user)
		RecipientCheckReport(items.toList)
	}

	def resolve(id: String, user: User) = user match {
		case FoundUser(user) => {
			if (user.getEmail.hasText && isGoodEmail(user.getEmail)) {
				GoodUser(user)
			} else {
				BadEmail(user)
			}
		}
		case NoUser(user) => MissingUser(id)
	}

	def isGoodEmail(email: String): Boolean = {
		try {
			new InternetAddress(email).validate
			true
		} catch {
			case e: MessagingException => false
		}
	}
}