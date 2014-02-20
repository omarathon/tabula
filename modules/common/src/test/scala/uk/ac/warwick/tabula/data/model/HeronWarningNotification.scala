package uk.ac.warwick.tabula.data.model

import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.tabula.data.model.groups.SmallGroup

object HeronWarningNotification {
	val templateLocation = "/WEB-INF/freemarker/notifications/i_really_hate_herons.ftl"
	val heronRant = "They are after your delicious eye jelly. Throw rocks at them!"
}

@Entity
@DiscriminatorValue(value="heronWarning")
class HeronWarningNotification extends NotificationWithTarget[SmallGroup, SmallGroup] with SingleItemNotification[SmallGroup]{

	import HeronWarningNotification._

	val verb: String = "Heron"

	def title: String = s"${item.entity.name} - You all need to know. Herons would love to kill you in your sleep"
	def content = FreemarkerModel(templateLocation, Map("group" -> item, "rant" -> heronRant))
	def url: String = "/beware/herons"
	def recipients = item.entity.students.users
}