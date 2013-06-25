package uk.ac.warwick.tabula.groups.notifications

import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.UserLookupService
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.web.views.{TextRenderer, FreemarkerRendering}
import freemarker.template.Configuration
object ReleaseSmallGroupSetsNotification{
  val templateLocation  = "/WEB-INF/freemarker/notifications/release_small_group_notification.ftl"
}

class ReleaseSmallGroupSetsNotification(private val groups:Seq[SmallGroup],
                                        val agent:User,
                                        val recipient:User,
                                        private val isStudent:Boolean ) extends Notification[Seq[SmallGroup]] with SingleRecipientNotification {

  this: TextRenderer=>
  import ReleaseSmallGroupSetsNotification._

  val verb: String = "Release"
  val _object: Seq[SmallGroup] = groups

  if (groups.isEmpty){
    throw new IllegalArgumentException("Attempted to create a ReleaseSmallGroupSetsNotification with no SmallGroups!")
  }
  val target: Option[AnyRef] = None

  def title: String = {
    val formatString = groups match {
      case Nil=> ""
      case singleGroup::Nil=>{
          singleGroup.groupSet.format.description
      }
      case _=>{
        val formats = groups.map(g=>g.groupSet.format.description).toList.distinct
        formats.init.mkString(", ") + " and " + formats.last
      }
    }
    formatString +  " allocation"
  }

  def content: String = {
    renderTemplate(templateLocation, Map("user"->recipient, "groups"->groups, "profileUrl"->url) )
  }
  def url: String = {
    if (isStudent){
      Routes.profile.view(recipient)
    }else{
      Routes.tutor.mygroups(recipient)
    }
  }

}
