package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.tabula.commands.{Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPoint, MonitoringPointSet}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent, AutowiringCourseAndRouteServiceComponent, CourseAndRouteServiceComponent}
import uk.ac.warwick.tabula.AcademicYear
import scala.collection.JavaConverters._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.{Daoisms, SessionComponent, AutowiringTransactionalComponent, TransactionalComponent}

object MonitoringPointSetFixtureCommand {
	def apply() =
		new MonitoringPointSetFixtureCommand()
		with ComposableCommand[MonitoringPointSet]
		with AutowiringCourseAndRouteServiceComponent
		with AutowiringMonitoringPointServiceComponent
		with AutowiringTransactionalComponent
		with Daoisms
		with PubliclyVisiblePermissions
		with Unaudited
}

class MonitoringPointSetFixtureCommand extends CommandInternal[MonitoringPointSet] {

	this: CourseAndRouteServiceComponent with MonitoringPointServiceComponent
		with TransactionalComponent with SessionComponent =>

	var routeCode: String = _
	var pointCount: Int = _
	var academicYear: AcademicYear = _
	var year: Int = _

	def applyInternal() = transactional() {

		val route = courseAndRouteService.getRouteByCode(routeCode).getOrElse(throw new IllegalArgumentException)

		// Delete existing
		monitoringPointService.findMonitoringPointSet(route, academicYear, Option(year)).map(set => {
			set.points.asScala.foreach(point => {
				point.checkpoints.asScala.foreach(checkpoint => session.delete(checkpoint))
				session.delete(point)
			})
			session.delete(set)
		})

		val pointSet = new MonitoringPointSet
		pointSet.academicYear = academicYear
		pointSet.route = route
		pointSet.year = year
		pointSet.createdDate = DateTime.now
		pointSet.updatedDate = DateTime.now

		pointSet.points = {
			for (count <- 0 until pointCount) yield {
				val point = new MonitoringPoint
				point.name = s"Point ${count+1}"
				point.createdDate = DateTime.now
				point.updatedDate = DateTime.now
				point.pointSet = pointSet
				point.validFromWeek = count + 1
				point.requiredFromWeek = count + 1
				point
			}
		}.asJava

		monitoringPointService.saveOrUpdate(pointSet)

		pointSet
	}

}
