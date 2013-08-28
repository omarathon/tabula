package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPoint, MonitoringPointSet}
import uk.ac.warwick.tabula.services.{TermServiceComponent, AutowiringTermServiceComponent, AutowiringRouteServiceComponent, RouteServiceComponent}
import uk.ac.warwick.tabula.data.model.{Route, Department}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.{ItemNotFoundException, AcademicYear}
import org.joda.time.{DateTimeConstants, DateMidnight, DateTime}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.JavaImports.JHashMap

object AddMonitoringPointSetCommand {
	def apply(dept: Department) =
		new AddMonitoringPointSetCommand(dept)
		with ComposableCommand[MonitoringPointSet]
		with AutowiringRouteServiceComponent
		with AutowiringTermServiceComponent
		with AddMonitoringPointSetPermissions
		with AddMonitoringPointSetDescription
		with AddMonitoringPointSetValidation
}


abstract class AddMonitoringPointSetCommand(val dept: Department) extends CommandInternal[MonitoringPointSet]
	with AddMonitoringPointSetState {
	self: RouteServiceComponent =>

	override def applyInternal() = {
		val set = new MonitoringPointSet
		set
	}
}

trait AddMonitoringPointSetValidation extends SelfValidating {
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
						selectedYears.filter(y => existingYears.filter(ey => ey.year.toString.equals(y)).size > 0).foreach(y => {
							errors.rejectValue("selectedRoutesAndYears",  "monitoringPointSet.duplicate", Array(y, route.code.toUpperCase), null)
						})
					}
				} else if (selectedYears.size > 1 && selectedYears.contains("All")) {
					errors.rejectValue("selectedRoutesAndYears", "monitoringPointSet.mixed", Array(route.code.toUpperCase), null)
				}
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

trait AddMonitoringPointSetDescription extends Describable[MonitoringPointSet] {
	self: AddMonitoringPointSetState =>

	override lazy val eventName = "AddMonitoringPointSet"

	override def describe(d: Description) {
		d.department(dept)
	}
}

trait AddMonitoringPointSetState extends TermServiceComponent with RouteServiceComponent {

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

	private def groupByTerm() = {
		lazy val weeksForYear =
			termService.getAcademicWeeksForYear(new DateMidnight(academicYear.startYear, DateTimeConstants.NOVEMBER, 1))
				.asScala.map { pair => pair.getLeft -> pair.getRight } // Utils pairs to Scala pairs
				.toMap
		val day = DayOfWeek(1)
		monitoringPoints.asScala.groupBy {
			case point => termService.getTermFromDateIncludingVacations(
				weeksForYear(point.week).getStart.withDayOfWeek(day.jodaDayOfWeek)
			).getTermTypeAsString
		}
	}

	def dept: Department
	val academicYear = AcademicYear.guessByDate(new DateTime())
	val availableRoutes = dept.routes.asScala.sortBy(r => r.code)
	lazy val availableYears = getAvailableYears
	val monitoringPoints = new java.util.ArrayList[MonitoringPoint]
	lazy val monitoringPointsByTerm = groupByTerm()
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
