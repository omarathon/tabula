package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.{FreemarkerModel, UserIdRecipientNotification, Notification}
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.web.Routes
import javax.persistence.{DiscriminatorValue, Entity}

object ReleaseSmallGroupSetsNotification {
	val templateLocation = "/WEB-INF/freemarker/notifications/groups/release_small_group_notification.ftl"
}

@Entity
@DiscriminatorValue("ReleaseSmallGroupSets")
class ReleaseSmallGroupSetsNotification extends Notification[SmallGroup, Unit]
	with UserIdRecipientNotification
	with AutowiringUserLookupComponent {

	def verb: String = "Release"

	override def onPreSave(newRecord: Boolean) {
		if (entities.isEmpty) {
			throw new IllegalArgumentException("Attempted to create a ReleaseSmallGroupSetsNotification with no SmallGroups!")
		}
	}

	def groups: Seq[SmallGroup] = entities

	def isStudent = getBooleanSetting("isStudent", default=false)
	def isStudent_= (b:Boolean) { settings += ("isStudent" -> b) }

	def formatString = groups.toList match {
		case Nil => ""
		case singleGroup :: Nil => singleGroup.groupSet.format.description
		case _ => {
			val formats = groups.map(g => g.groupSet.format.description).toList.distinct
			formats.init.mkString(", ") + " and " + formats.last
		}
	}

	def title: String = s"$formatString allocation"

	def content =
		FreemarkerModel(ReleaseSmallGroupSetsNotification.templateLocation,
			Map("user" -> recipient, "groups" -> groups, "profileUrl" -> url)
		)

	def actionRequired = false
	
	def url: String = {
		if (isStudent) {
			Routes.profiles.profile.mine
		} else {
			Routes.groups.tutor.mygroups
		}
	}

	def urlTitle = s"view your $formatString groups"

}
