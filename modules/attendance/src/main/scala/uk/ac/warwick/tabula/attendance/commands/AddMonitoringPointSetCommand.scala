package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.{AbstractMonitoringPointSet, MonitoringPoint, MonitoringPointSet}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.{Route, Department}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.{ItemNotFoundException, AcademicYear}
import org.joda.time.DateTime
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports.JHashMap
import org.springframework.util.AutoPopulatingList
import scala.Some

object AddMonitoringPointSetCommand {
	def apply(dept: Department, existingSetOption: Option[AbstractMonitoringPointSet]) =
		new AddMonitoringPointSetCommand(dept, existingSetOption)
		with ComposableCommand[Seq[MonitoringPointSet]]
		with AutowiringRouteServiceComponent
		with AutowiringTermServiceComponent
		with AutowiringMonitoringPointServiceComponent
		with AddMonitoringPointSetPermissions
		with AddMonitoringPointSetDescription
		with AddMonitoringPointSetValidation
}


abstract class AddMonitoringPointSetCommand(val dept: Department, val existingSetOption: Option[AbstractMonitoringPointSet]) extends CommandInternal[Seq[MonitoringPointSet]]
	with AddMonitoringPointSetState {
	self: MonitoringPointServiceComponent =>

	override def applyInternal() = {
		selectedRoutesAndYears.asScala.map{case (route, allYears) => {
				allYears.asScala.filter(_._2).keySet.map(year => {
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
				monitoringPointService.saveOrUpdate(set)
				set
			})
		}}.flatten.toSeq
	}
}

trait AddMonitoringPointSetValidation extends SelfValidating with MonitoringPointValidation {
	self: AddMonitoringPointSetState =>

	override def validate(errors: Errors) {
		selectedRoutesAndYears.asScala.map{case (route, allYears) => {
			val selectedYears = allYears.asScala.filter(_._2).keySet
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
		}}
		if (selectedRoutesAndYears.asScala.count(_._2.asScala.count(_._2) > 0) == 0) {
			errors.rejectValue("selectedRoutesAndYears", "monitoringPointSet.noYears")
		}
		if (monitoringPoints.size() == 0) {
			errors.rejectValue("monitoringPoints", "monitoringPointSet.noPoints")
		}

		monitoringPoints.asScala.zipWithIndex.foreach{case (point, index) => {
			validateName(errors, point.name, s"monitoringPoints[$index].name")
			validateWeek(errors, point.week, s"monitoringPoints[$index].week")

			if (monitoringPoints.asScala.count(p => p.name == point.name && p.week == point.week) > 1) {
				errors.rejectValue(s"monitoringPoints[$index].name", "monitoringPoint.name.exists")
			}
		}}

		// when changing year fail validation so nothing is committed
		if (changeYear) {
			errors.reject("")
		}
	}
}

trait AddMonitoringPointSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AddMonitoringPointSetState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, mandatory(dept))
	}
}

trait AddMonitoringPointSetDescription extends Describable[Seq[MonitoringPointSet]] {
	self: AddMonitoringPointSetState =>

	override lazy val eventName = "AddMonitoringPointSet"

	override def describe(d: Description) {
		d.department(dept)
		d.property("routesAndYears", selectedRoutesAndYears.asScala.map{ case	(route, allYears) =>
			route.code -> allYears.asScala.filter(_._2).keys
		}.filter{case	(route, selectedYears) =>  selectedYears.size > 0})
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
		for {
			r <- dept.routes.asScala
			existingSet <- r.monitoringPointSets.asScala.filter(s => s.academicYear == academicYear)
		}	yield {
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

	def existingSetOption: Option[AbstractMonitoringPointSet]

	var academicYear = AcademicYear.guessByDate(new DateTime())

	var changeYear = false

	val availableRoutes = dept.routes.asScala.sortBy(r => r.code)

	lazy val availableYears = getAvailableYears

	val pointSetToCopy = null

	val monitoringPoints = existingSetOption match {
		case Some(set: AbstractMonitoringPointSet) => {
			val points = new AutoPopulatingList(classOf[MonitoringPoint])
			set.points.asScala.foreach(p => points.add(p))
			points
		}
		case None => new AutoPopulatingList(classOf[MonitoringPoint])
	}

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

	def selectedRoutesAndYearsByRouteCode(route: Route) = selectedRoutesAndYears.get(route)
}
