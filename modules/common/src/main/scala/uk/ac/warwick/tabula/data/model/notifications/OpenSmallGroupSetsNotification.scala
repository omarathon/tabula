package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.{UserIdRecipientNotification, FreemarkerModel, Notification}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import javax.persistence.{Entity, DiscriminatorValue}
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

object OpenSmallGroupSetsNotification {
	@transient val templateLocation = "/WEB-INF/freemarker/notifications/open_small_group_student_notification.ftl"
}

@Entity
@DiscriminatorValue(value="OpenSmallGroupSets")
class OpenSmallGroupSetsNotification
	extends Notification[SmallGroupSet, Unit]
	with UserIdRecipientNotification
	with AutowiringUserLookupComponent {

	def verb = "Opened"

	def title: String = {
		val formats: List[String] = entities.map(_.format.description).distinct.toList

		val formatsString = formats match {
			case singleFormat :: Nil => singleFormat
			case _ => Seq(formats.init.mkString(", "), formats.last).mkString(" and ")
		}
		formatsString + " groups are now open for sign up."
	}

	def content = FreemarkerModel(OpenSmallGroupSetsNotification.templateLocation, Map("groupsets" -> entities, "profileUrl" -> url))

	def url: String = "/groups"

}
