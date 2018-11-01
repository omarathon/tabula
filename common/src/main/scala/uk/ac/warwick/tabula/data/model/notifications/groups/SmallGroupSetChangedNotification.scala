package uk.ac.warwick.tabula.data.model.notifications.groups

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, MyWarwickActivity, NotificationWithTarget, UserIdRecipientNotification}
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.web.Routes

object SmallGroupSetChangedNotification {
	val templateLocation = "/WEB-INF/freemarker/notifications/groups/small_group_modified_notification.ftl"
}

trait SmallGroupSetChangedUserIdRecipientNotification
	extends NotificationWithTarget[SmallGroup, SmallGroupSet]
	with UserIdRecipientNotification
	with AutowiringUserLookupComponent
	with MyWarwickActivity {

	def verb = "Modify"

	def title: String = "%s: Your %s allocation has changed".format(target.entity.module.code.toUpperCase, target.entity.format.description.toLowerCase)

	@transient val oldSmallGroupSizes = StringMapSetting("oldSmallGroupSizes", Map())

	def content =
		FreemarkerModel(SmallGroupSetChangedNotification.templateLocation, Map(
			"groups" -> entities,
			"groupSet" -> target.entity,
			"profileUrl" -> url
		) ++ extraModel)

	def urlTitle = "view this small group"

	def extraModel: Map[String, Any] = Map()
}

@Entity
@DiscriminatorValue(value="SmallGroupSetChangedStudent")
class SmallGroupSetChangedStudentNotification extends SmallGroupSetChangedUserIdRecipientNotification {

	def url: String = Routes.profiles.Profile.events(recipient.getWarwickId)

}

@Entity
@DiscriminatorValue(value="SmallGroupSetChangedTutor")
class SmallGroupSetChangedTutorNotification extends SmallGroupSetChangedUserIdRecipientNotification {

	def url: String = Routes.groups.tutor.mygroups

	private def changedGroupInfo = {
		entities.map { newSmallGroup =>
			(newSmallGroup, oldSmallGroupSizes.value.getOrElse(newSmallGroup.id, "0"))
		}.filter { case (newGroup,oldSize) => newGroup.students.size != oldSize.toInt }
	}

	override  def extraModel = Map("groupsWithOldSizeInfo" -> changedGroupInfo)
}