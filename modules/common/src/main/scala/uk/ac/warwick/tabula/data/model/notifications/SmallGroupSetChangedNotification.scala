package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.groups.{ SmallGroup, SmallGroupSet }
import uk.ac.warwick.tabula.data.model.{NotificationWithTarget, UserIdRecipientNotification, FreemarkerModel}
import javax.persistence.{Entity, DiscriminatorValue}
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

object SmallGroupSetChangedNotification {
	val templateLocation = "/WEB-INF/freemarker/notifications/small_group_modified_notification.ftl"
}

abstract class SmallGroupSetChangedNotification(recipientRole: UserRoleOnGroup)
	extends NotificationWithTarget[SmallGroup, SmallGroupSet]
	with UserIdRecipientNotification
	with AutowiringUserLookupComponent {

	def verb: String = "Modify"

	def title: String = "Changes to small group allocation"

	def content =
		FreemarkerModel(SmallGroupSetChangedNotification.templateLocation, Map(
			"groups" -> entities,
			"groupSet" -> target.entity,
			"profileUrl" -> url
		))

	def url: String = {
		recipientRole match {
			case UserRoleOnGroup.Student =>Routes.profiles.profile.mine
			case UserRoleOnGroup.Tutor => Routes.groups.tutor.mygroups
		}
	}

	def urlTitle = "view this group"

}

@Entity
@DiscriminatorValue(value="SmallGroupSetChangedStudent")
class SmallGroupSetChangedStudentNotification extends SmallGroupSetChangedNotification(UserRoleOnGroup.Student)

@Entity
@DiscriminatorValue(value="SmallGroupSetChangedTutor")
class SmallGroupSetChangedTutorNotification extends SmallGroupSetChangedNotification(UserRoleOnGroup.Tutor)

sealed trait UserRoleOnGroup
object UserRoleOnGroup {
	case object Student extends UserRoleOnGroup
	case object Tutor extends UserRoleOnGroup
}

