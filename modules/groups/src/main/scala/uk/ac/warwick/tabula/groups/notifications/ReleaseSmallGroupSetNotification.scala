package uk.ac.warwick.tabula.groups.notifications

import uk.ac.warwick.tabula.data.model.{ SingleRecipientNotification, Notification }
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.web.views.TextRenderer

class ReleaseSmallGroupSetNotification(private val group: SmallGroup, val agent: User, val recipient: User, private val isStudent: Boolean)
	extends Notification[SmallGroup] with SingleRecipientNotification {

	this: TextRenderer =>

	val templateLocation = "/WEB-INF/freemarker/notifications/release_small_group_student_notification.ftl"

	val verb: String = "Release"
	val _object: SmallGroup = group
	val target: Option[AnyRef] = None

	def title: String = group.groupSet.format.description + " allocation"

	def content: String = {
		renderTemplate(templateLocation, Map("user" -> recipient, "group" -> group, "profileUrl" -> url))
	}
	def url: String = {
		if (isStudent) {
			"/profiles" + Routes.profile.view(recipient)
		} else {
			"/groups" + Routes.tutor.mygroups(recipient)
		}
	}

}
