package uk.ac.warwick.tabula.groups.notifications

import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.web.views.TextRenderer
import scala.collection.JavaConverters._

object SmallGroupSetChangedNotification {
  val templateLocation =  "/WEB-INF/freemarker/notifications/small_group_modified_student_notification.ftl"
}
class SmallGroupSetChangedNotification(val group:SmallGroup, val agent:User, val recipient:User) extends Notification[SmallGroup] with SingleRecipientNotification{

  this: TextRenderer =>
   import SmallGroupSetChangedNotification._

  val verb: String = "Modify"

  val _object: SmallGroup = group
  val target: Option[AnyRef] = None

  def title: String = "Changes to small group allocation"

  def content: String = {
    renderTemplate(templateLocation, Map("group"->group, "student"->recipient, "profileUrl"->url))
  }

  def url: String =  Routes.profile.view(recipient)
}
