package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.{UserIdRecipientNotification, FreemarkerModel, Notification}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import javax.persistence.{Entity, DiscriminatorValue}
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod.StudentSignUp
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning

object OpenSmallGroupSetsNotification {
	@transient val templateLocation = "/WEB-INF/freemarker/notifications/groups/open_small_group_student_notification.ftl"
}

@Entity
@DiscriminatorValue(value="OpenSmallGroupSets")
class OpenSmallGroupSetsNotification
	extends Notification[SmallGroupSet, Unit]
	with UserIdRecipientNotification
	with AutowiringUserLookupComponent {

	override def onPreSave(isNew: Boolean) {
		// if any of the groups require the student to sign up then the priority should be higher
		if (entities.exists(_.allocationMethod == StudentSignUp)) {
			 priority = Warning
		}
	}

	def actionRequired = entities.exists(_.allocationMethod == StudentSignUp)

	def verb = "Opened"

	def formats: List[String] = entities.map(_.format.description).distinct.toList

	def formatsString = formats match {
		case singleFormat :: Nil => singleFormat
		case _ => Seq(formats.init.mkString(", "), formats.last).mkString(" and ")
	}

	def title: String = formatsString + " groups are now open for sign up."

	def content = FreemarkerModel(OpenSmallGroupSetsNotification.templateLocation, Map(
		"groupsets" -> entities,
		"profileUrl" -> url,
		"formatsString" -> formatsString
	))

	def url: String = "/groups"
	def urlTitle = s"sign up for these $formatsString groups"

}
