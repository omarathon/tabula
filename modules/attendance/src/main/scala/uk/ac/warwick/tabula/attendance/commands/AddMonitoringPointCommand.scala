package uk.ac.warwick.tabula.attendance.commands

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.services.{AutowiringRouteServiceComponent, RouteServiceComponent}

object AddMonitoringPointCommand {
	def apply(set: MonitoringPointSet) =
		new AddMonitoringPointCommand(set)
		with ComposableCommand[MonitoringPoint]
		with AddMonitoringPointValidation
		with ModifyMonitoringPointPermissions
		with ModifyMonitoringPointState
		with AddMonitoringPointDescription
		with AutowiringRouteServiceComponent
}

/**
 * Creates a new monitoring point in a set.
 */
class AddMonitoringPointCommand(val set: MonitoringPointSet) extends CommandInternal[MonitoringPoint] {
	self: ModifyMonitoringPointState with RouteServiceComponent =>

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
		week match {
			case y if y < 1  => errors.rejectValue("week", "monitoringPointSet.week.min")
			case y if y > 52 => errors.rejectValue("week", "monitoringPointSet.week.max")
			case _ =>
		}

		if (!name.hasText) {
			errors.rejectValue("name", "NotEmpty")
		} else if (name.length > 4000) {
			errors.rejectValue("name", "monitoringPoint.name.toolong")
		}

		if (set.points.asScala.filter(p => p.name == name && p.week == week).size > 0) {
			errors.rejectValue("name", "monitoringPoint.name.exists")
			errors.rejectValue("week", "monitoringPoint.name.exists")
		}
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

