package uk.ac.warwick.tabula.home.commands.sysadmin.pointsettemplates

import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors
import scala.collection.JavaConverters._
import org.springframework.util.AutoPopulatingList

object EditMonitoringPointCommand {
	def apply(pointIndex: Int) =
		new EditMonitoringPointCommand(pointIndex)
		with ComposableCommand[Unit]
		with MonitoringPointSetTemplatesPermissions
		with EditMonitoringPointValidation
		with ReadOnly with Unaudited
}

/**
 * Edits an existing monitoring point from the set of points in the command's state.
 * Does not persist the change (no monitoring point set yet exists)
 */
abstract class EditMonitoringPointCommand(val pointIndex: Int)
	extends CommandInternal[Unit] with EditMonitoringPointState {

	override def applyInternal() = {
		copyTo(monitoringPoints.get(pointIndex))
	}
}

trait EditMonitoringPointValidation extends SelfValidating with MonitoringPointValidation {
	self: EditMonitoringPointState =>

	override def validate(errors: Errors) {
		validateWeek(errors, week, "week")
		validateName(errors, name, "name")

		val pointsWithCurrentRemoved = monitoringPoints.asScala.zipWithIndex.filter(_._2 != pointIndex).unzip._1
		if (pointsWithCurrentRemoved.count(p => p.name == name && p.week == week) > 0) {
			errors.rejectValue("name", "monitoringPoint.name.exists")
			errors.rejectValue("week", "monitoringPoint.name.exists")
		}
	}
}

trait EditMonitoringPointState {
	val pointIndex: Int
	var monitoringPoints = new AutoPopulatingList(classOf[MonitoringPoint])
	var name: String = _
	var defaultValue: Boolean = true
	var week: Int = 0

	def copyTo(point: MonitoringPoint) {
		point.name = this.name
		point.defaultValue = this.defaultValue
		point.week = this.week
	}

	def copyFrom() {
		val point = monitoringPoints.get(pointIndex)
		this.name = point.name
		this.defaultValue = point.defaultValue
		this.week = point.week
	}
}

