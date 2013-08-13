package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors

object AddMonitoringPointCommand {
	def apply(set: MonitoringPointSet) =
		new AddMonitoringPointCommand(set)
		with ComposableCommand[MonitoringPoint]
		with AddMonitoringPointValidation
		with ModifyMonitoringPointPermissions
		with ModifyMonitoringPointState
		with AddMonitoringPointDescription
}

/**
 * Creates a new monitoring point in a set.
 */
class AddMonitoringPointCommand(val set: MonitoringPointSet) extends ModifyMonitoringPointCommand {
	self: ModifyMonitoringPointState =>

	override def applyInternal() = {
		val point = new MonitoringPoint
		this.copyTo(point)
		set.add(point)
		point
	}
}



trait AddMonitoringPointValidation extends SelfValidating {
	self: ModifyMonitoringPointState =>

	override def validate(errors: Errors) {

	}
}

trait AddMonitoringPointDescription extends Describable[MonitoringPoint] {
	self: ModifyMonitoringPointState =>

	override lazy val eventName = "AddMonitoringPoint"

	override def describe(d: Description) {
		d.monitoringPointSet(set)
		d.property("name", name)
	}
}

