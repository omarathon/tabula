package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPoint, MonitoringPointSet}
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, AutowiringRouteServiceComponent, RouteServiceComponent}
import uk.ac.warwick.tabula.data.model.{Route, Department}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.{ItemNotFoundException, AcademicYear}
import org.joda.time.DateTime
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports.JHashMap
import org.springframework.util.AutoPopulatingList
import scala.collection.mutable

object AddMonitoringPointSetCommand {
	def apply(dept: Department) =
		new AddMonitoringPointSetCommand(dept)
		with ComposableCommand[mutable.Buffer[MonitoringPointSet]]
		with AutowiringRouteServiceComponent
		with AutowiringTermServiceComponent
		with AddMonitoringPointSetPermissions
		with AddMonitoringPointSetDescription
		with AddMonitoringPointSetValidation
}


abstract class AddMonitoringPointSetCommand(val dept: Department) extends CommandInternal[mutable.Buffer[MonitoringPointSet]]
	with AddMonitoringPointSetState {
	self: RouteServiceComponent =>

	override def applyInternal() = {
		val sets: mutable.Buffer[MonitoringPointSet] = mutable.Buffer()
		selectedRoutesAndYears.asScala.foreach(pair => {
			val route = pair._1
			val selectedYears = pair._2.asScala.filter(p => p._2).keySet
			selectedYears.foreach(year => {
				val set = new MonitoringPointSet
				set.academicYear = academicYear
				set.createdDate = new DateTime()
				set.points = monitoringPoints.asScala.map{m =>
					val point = new MonitoringPoint
					point.createdDate = new DateTime()
					point.defaultValue = m.defaultValue
					point.name = m.name
					point.pointSet = set
					point.updatedDate = new DateTime()
					point.week = m.week
					point
				}.asJava
				set.route = route
				set.updatedDate = new DateTime()
				set.year = if (year.equals("All")) null else year.toInt
				routeService.save(set)
				sets.append(set)
			})
		})
		sets
	}
}

trait AddMonitoringPointSetValidation extends SelfValidating with MonitoringPointValidation {
	self: AddMonitoringPointSetState with RouteServiceComponent =>

	override def validate(errors: Errors) {
		selectedRoutesAndYears.asScala.foreach(pair => {
			val route = pair._1
			val selectedYears = pair._2.asScala.filter(p => p._2).keySet
			if (selectedYears.size > 0) {
				val existingYears = route.monitoringPointSets.asScala.filter(s => s.academicYear == academicYear)
				if (existingYears.size == 1 && existingYears.head.year == null) {
					errors.rejectValue("selectedRoutesAndYears", "monitoringPointSet.allYears", Array(route.code.toUpperCase), null)
				} else if (existingYears.size > 0) {
					if (selectedYears.contains("All")) {
						errors.rejectValue("selectedRoutesAndYears", "monitoringPointSet.alreadyYear", Array(route.code.toUpperCase), null)
					} else {
						selectedYears.filter(y => existingYears.count(ey => ey.year.toString.equals(y)) > 0).foreach(y => {
							errors.rejectValue("selectedRoutesAndYears",  "monitoringPointSet.duplicate", Array(y, route.code.toUpperCase), null)
						})
					}
				} else if (selectedYears.size > 1 && selectedYears.contains("All")) {
					errors.rejectValue("selectedRoutesAndYears", "monitoringPointSet.mixed", Array(route.code.toUpperCase), null)
				}
			}
		})
		if (selectedRoutesAndYears.asScala.count(p => p._2.asScala.count(y => y._2) > 0) == 0) {
			errors.rejectValue("selectedRoutesAndYears", "monitoringPointSet.noYears")
		}
		if (monitoringPoints.size() == 0) {
			errors.rejectValue("monitoringPoints", "monitoringPointSet.noPoints")
		}

		monitoringPoints.asScala.zipWithIndex.foreach(pair => {
			validateName(errors, pair._1.name, s"monitoringPoints[${pair._2}].name")
			validateWeek(errors, pair._1.week, s"monitoringPoints[${pair._2}].week")

			if (monitoringPoints.asScala.count(p => p.name == pair._1.name && p.week == pair._1.week) > 1) {
				errors.rejectValue(s"monitoringPoints[${pair._2}].name", "monitoringPoint.name.exists")
			}
		})
	}
}

trait AddMonitoringPointSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AddMonitoringPointSetState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, mandatory(dept))
	}
}

trait AddMonitoringPointSetDescription extends Describable[mutable.Buffer[MonitoringPointSet]] {
	self: AddMonitoringPointSetState =>

	override lazy val eventName = "AddMonitoringPointSet"

	override def describe(d: Description) {
		d.department(dept)
		d.property("routesAndYears", selectedRoutesAndYears.asScala.map{
			pair => pair._1.code -> pair._2.asScala.filter(p => p._2).keys
		}.filter{pair => pair._2.size > 0})
	}
}



trait AddMonitoringPointSetState extends GroupMonitoringPointsByTerm with RouteServiceComponent {

	private def getAvailableYears = {
		val routeMap = dept.routes.asScala.map {
			r => r.code -> collection.mutable.Map(
				"1" -> true,
				"2" -> true,
				"3" -> true,
				"4" -> true,
				"5" -> true,
				"6" -> true,
				"7" -> true,
				"8" -> true,
				"All" -> true
			)
		}.toMap
		for (r <- dept.routes.asScala; existingSet <- r.monitoringPointSets.asScala.filter(s => s.academicYear == academicYear))
			yield {
			if (existingSet.year ==  null) {
				routeMap(r.code).foreach(p => routeMap(r.code)(p._1) = false)
			} else {
				routeMap(r.code)(existingSet.year.toString) = false
				routeMap(r.code)("All") = false
			}
		}
		routeMap
	}

	def dept: Department
	var academicYear = AcademicYear.guessByDate(new DateTime())
	val availableRoutes = dept.routes.asScala.sortBy(r => r.code)
	lazy val availableYears = getAvailableYears
	val monitoringPoints = new AutoPopulatingList(classOf[MonitoringPoint])
	def monitoringPointsByTerm = groupByTerm(monitoringPoints.asScala, academicYear)
	val selectedRoutesAndYears: java.util.Map[Route, java.util.HashMap[String, java.lang.Boolean]] = dept.routes.asScala.map {
		r => r -> JHashMap(
			"1" -> java.lang.Boolean.FALSE,
			"2" -> java.lang.Boolean.FALSE,
			"3" -> java.lang.Boolean.FALSE,
			"4" -> java.lang.Boolean.FALSE,
			"5" -> java.lang.Boolean.FALSE,
			"6" -> java.lang.Boolean.FALSE,
			"7" -> java.lang.Boolean.FALSE,
			"8" -> java.lang.Boolean.FALSE,
			"All" -> java.lang.Boolean.FALSE
		)
	}.toMap.asJava
	def selectedRoutesAndYearsByRouteCode(code: String) = routeService.getByCode(code) match {
		case Some(r: Route) => selectedRoutesAndYears.get(r)
		case _ => new ItemNotFoundException()

	}

}
