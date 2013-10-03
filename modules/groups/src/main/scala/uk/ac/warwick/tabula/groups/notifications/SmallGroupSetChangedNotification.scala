package uk.ac.warwick.tabula.groups.notifications

import uk.ac.warwick.tabula.data.model.groups.{ SmallGroup, SmallGroupSet }
import uk.ac.warwick.tabula.data.model.{ SingleRecipientNotification, Notification }
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.web.views.TextRenderer
import scala.collection.JavaConverters._

object SmallGroupSetChangedNotification {
	val templateLocation = "/WEB-INF/freemarker/notifications/small_group_modified_notification.ftl"
}
class SmallGroupSetChangedNotification(val groupSet: SmallGroupSet, val agent: User, val recipient: User, recipientRole: UserRoleOnGroup)
	extends Notification[SmallGroupSet] with SingleRecipientNotification {

	this: TextRenderer =>
	import SmallGroupSetChangedNotification._

	val verb: String = "Modify"

	val _object: SmallGroupSet = groupSet
	val target: Option[AnyRef] = None

	def title: String = "Changes to small group allocation"

	def content: String = {
		renderTemplate(templateLocation, Map("groupSet" -> groupSet, "profileUrl" -> url))
	}

	def url: String = {
		recipientRole match {
			case UserRoleOnGroup.Student => "/profiles" + Routes.profile.view(recipient)
			case UserRoleOnGroup.Tutor => "/groups" + Routes.tutor.mygroups(recipient)
		}
	}
}
sealed trait UserRoleOnGroup
object UserRoleOnGroup {
	case object Student extends UserRoleOnGroup
	case object Tutor extends UserRoleOnGroup
}

