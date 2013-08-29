package uk.ac.warwick.tabula.groups.notifications

import uk.ac.warwick.tabula.data.model.{SingleRecipientNotification, Notification}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.web.views.TextRenderer

object OpenSmallGroupSetsNotification {
	val templateLocation = "/WEB-INF/freemarker/notifications/open_small_group_student_notification.ftl"

}

class OpenSmallGroupSetsNotification(val agent: User, val recipient: User, val _object: Seq[SmallGroupSet])
	extends Notification[Seq[SmallGroupSet]] with SingleRecipientNotification {

	this: TextRenderer =>

	import OpenSmallGroupSetsNotification._
	val verb: String = "Opened"
	val target: Option[AnyRef] = None

	def title: String = {
		val formats = _object.map(_.format.description).distinct

		val formatsString = formats match {
			case singleFormat::Nil =>singleFormat
			case multipleFormats => Seq(formats.init.mkString(", "),formats.last).mkString(" and ")
		}
		formatsString + " groups are now open for sign up."
	}

	def content: String = renderTemplate(templateLocation, Map("user" -> recipient, "groupsets" -> _object, "profileUrl" -> url))

	def url: String = "/"

}
