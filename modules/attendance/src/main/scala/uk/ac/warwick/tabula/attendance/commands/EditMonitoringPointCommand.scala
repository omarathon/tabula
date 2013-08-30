package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import uk.ac.warwick.tabula.data.model.Department
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.system.permissions.Public
import org.springframework.util.AutoPopulatingList
import uk.ac.warwick.tabula.helpers.StringUtils._

object EditMonitoringPointCommand {
	def apply(dept: Department, pointIndex: Int) =
		new EditMonitoringPointCommand(dept, pointIndex)
		with AutowiringTermServiceComponent
		with EditMonitoringPointValidation
		with Public
}

/**
 * Edits an existing monitoring point from the set of points in the command's state.
 * Does not persist the change (no monitoring point set yet exists)
 */
abstract class EditMonitoringPointCommand(val dept: Department, val pointIndex: Int)
	extends Command[Unit] with ReadOnly with Unaudited with EditMonitoringPointState {

	override def applyInternal() = {
		copyTo(monitoringPoints.get(pointIndex))
	}
}

trait EditMonitoringPointValidation extends SelfValidating {
	self: EditMonitoringPointState =>

	override def validate(errors: Errors) {
		week match {
			case y if y < 1  => errors.rejectValue("week", "monitoringPoint.week.min")
			case y if y > 52 => errors.rejectValue("week", "monitoringPoint.week.max")
			case _ =>
		}

		if (!name.hasText) {
			errors.rejectValue("name", "NotEmpty")
		} else if (name.length > 4000) {
			errors.rejectValue("name", "monitoringPoint.name.toolong")
		}

		val pointsWithCurrentRemoved = monitoringPoints.asScala.zipWithIndex.filter(_._2 != pointIndex).unzip._1
		if (pointsWithCurrentRemoved.count(p => p.name == name && p.week == week) > 0) {
			errors.rejectValue("name", "monitoringPoint.name.exists")
			errors.rejectValue("week", "monitoringPoint.name.exists")
		}
	}
}

trait EditMonitoringPointState extends GroupMonitoringPointsByTerm {
	val dept: Department
	val pointIndex: Int
	var monitoringPoints = new AutoPopulatingList(classOf[MonitoringPoint])
	var name: String = _
	var defaultValue: Boolean = true
	var week: Int = 0
	var academicYear: AcademicYear = AcademicYear.guessByDate(new DateTime())
	def monitoringPointsByTerm = groupByTerm(monitoringPoints.asScala, academicYear)

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

