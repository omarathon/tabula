package uk.ac.warwick.tabula.data.model.triggers

import javax.persistence._
import javax.validation.constraints.NotNull

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.{EntityReference, GeneratedId, ToEntityReference}

@Entity(name = "ScheduledTrigger")
@DiscriminatorColumn(name = "TRIGGER_TYPE", discriminatorType = DiscriminatorType.STRING)
abstract class Trigger[A >: Null <: ToEntityReference, B] extends GeneratedId with Serializable with Appliable[B] {

	@Column(name="scheduled_date")
	@NotNull
	var scheduledDate: DateTime = null

	@transient private[this] var _target: EntityReference[A] = _

	@Access(value=AccessType.PROPERTY)
	@OneToOne(cascade = Array(CascadeType.ALL), targetEntity = classOf[EntityReference[A]], fetch = FetchType.LAZY)
	def getTarget: EntityReference[A] = _target
	def target: EntityReference[A] = getTarget
	def setTarget(target: EntityReference[A]): Unit = _target = target
	def target_=(target: EntityReference[A]): Unit = setTarget(target)

	@Column(name="completed_date")
	var completedDate: DateTime = null

	def updateTarget(targetEntity: ToEntityReference): Unit = {
		target = Option(targetEntity).map { e =>
			e.toEntityReference.asInstanceOf[EntityReference[A]]
		}.orNull
	}
}
