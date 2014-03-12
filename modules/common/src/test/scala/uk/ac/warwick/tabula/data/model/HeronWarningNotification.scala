package uk.ac.warwick.tabula.data.model

import javax.persistence.{ManyToOne, DiscriminatorValue, Entity}
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.userlookup.User
import org.hibernate.annotations.Type

object HeronWarningNotification {
	val templateLocation = "/WEB-INF/freemarker/notifications/i_really_hate_herons.ftl"
	val heronRant = "They are after your delicious eye jelly. Throw rocks at them!"
}

@Entity
@DiscriminatorValue(value="heronWarning")
class HeronWarningNotification extends NotificationWithTarget[Heron, Heron]
	with SingleItemNotification[Heron] with SingleRecipientNotification {

	import HeronWarningNotification._

	val verb: String = "Heron"

	def title: String = "You all need to know. Herons would love to kill you in your sleep"
	def content = FreemarkerModel(templateLocation, Map("group" -> item, "rant" -> heronRant))
	def url: String = "/beware/herons"
	def recipient = item.entity.victim
}

@Entity
class Heron extends GeneratedId with ToEntityReference {

	def this(v: User) = {
		this()
		victim = v
	}

	type Entity = Heron
	def toEntityReference = new HeronEntityReference().put(this)

	@Type(`type`="uk.ac.warwick.tabula.data.model.SSOUserType")
	var victim: User = null
}

@Entity @DiscriminatorValue(value="heron")
class HeronEntityReference extends EntityReference[Heron] {
	@ManyToOne()
	var entity: Entity = null
}
