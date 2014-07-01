package uk.ac.warwick.tabula.attendance.commands.manage.old

import org.joda.time.DateTime
import org.springframework.util.AutoPopulatingList
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.JavaImports.JHashMap
import uk.ac.warwick.tabula.attendance.commands.old.{AutowiringSecurityServicePermissionsAwareRoutes, GroupMonitoringPointsByTerm, PermissionsAwareRoutes}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPoint, MonitoringPointSet, MonitoringPointSetTemplate, MonitoringPointType}
import uk.ac.warwick.tabula.data.model.{Department, Route}
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._
import scala.collection.mutable

object AddMonitoringPointSetCommand {
	def apply(
		user: CurrentUser,
		dept: Department,
		academicYear: AcademicYear,
		existingSetOption: Option[MonitoringPointSet],
		template: Option[MonitoringPointSetTemplate]
	) =
		new AddMonitoringPointSetCommand(user, dept, academicYear, existingSetOption, template)
			with ComposableCommand[Seq[MonitoringPointSet]]
			with AutowiringSecurityServicePermissionsAwareRoutes
			with AutowiringCourseAndRouteServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringMonitoringPointServiceComponent
			with AddMonitoringPointSetPermissions
			with AddMonitoringPointSetDescription
			with AddMonitoringPointSetValidation
}


abstract class AddMonitoringPointSetCommand(
	val user: CurrentUser,
	val dept: Department,
	val academicYear: AcademicYear,
	val existingSetOption: Option[MonitoringPointSet],
	val template: Option[MonitoringPointSetTemplate]
)	extends CommandInternal[Seq[MonitoringPointSet]] with AddMonitoringPointSetState {

	self: MonitoringPointServiceComponent =>

	assert(academicYear.startYear <= 2013, "Tried to create a point set for 2014/15")

	override def applyInternal() = {
		selectedRoutesAndYears.asScala.map{case (route, allYears) =>
				allYears.asScala.filter(_._2).keySet.map(year => {
				val set = new MonitoringPointSet
				set.academicYear = academicYear
				set.createdDate = new DateTime()
				set.points = monitoringPoints.asScala.map{m =>
					val point = new MonitoringPoint
					point.createdDate = new DateTime()
					point.name = m.name
					point.pointSet = set
					point.updatedDate = new DateTime()
					point.validFromWeek = m.validFromWeek
					point.requiredFromWeek = m.requiredFromWeek
					point.pointType = m.pointType
					point.meetingRelationships = m.meetingRelationships
					point.meetingFormats = m.meetingFormats
					point.meetingQuantity = m.meetingQuantity
					point
				}.asJava
				set.route = route
				set.updatedDate = new DateTime()
				set.year = if (year.equals("All")) null else year.toInt
				monitoringPointService.saveOrUpdate(set)
				set
			})
		}.flatten.toSeq
	}
}

trait AddMonitoringPointSetValidation extends SelfValidating with MonitoringPointValidation {
	self: AddMonitoringPointSetState =>

	override def validate(errors: Errors) {
		selectedRoutesAndYears.asScala.map{case (route, allYears) =>
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
		}
		if (selectedRoutesAndYears.asScala.count(_._2.asScala.count(_._2) > 0) == 0) {
			errors.rejectValue("selectedRoutesAndYears", "monitoringPointSet.noYears")
		}
		if (monitoringPoints.size() == 0) {
			errors.rejectValue("monitoringPoints", "monitoringPointSet.noPoints")
		}

		monitoringPoints.asScala.zipWithIndex.foreach{case (point, index) =>
			errors.pushNestedPath(s"monitoringPoints[$index]")
			validateName(errors, point.name, "name")
			validateWeek(errors, point.validFromWeek, "validFromWeek")
			validateWeek(errors, point.requiredFromWeek, "requiredFromWeek")
			validateWeeks(errors, point.validFromWeek, point.requiredFromWeek, "validFromWeek")

			point.pointType match {
				case MonitoringPointType.Meeting =>
					validateTypeMeeting(errors,
						mutable.Set(point.meetingRelationships).flatten, "meetingRelationships",
						mutable.Set(point.meetingFormats).flatten, "meetingFormats",
						point.meetingQuantity, "meetingQuantity",
						dept
					)
				case MonitoringPointType.SmallGroup =>
					validateTypeSmallGroup(errors,
						point.smallGroupEventModules.toSet.asJava, "smallGroupEventModules",
						isAnySmallGroupEventModules = false,
						point.meetingQuantity, "smallGroupEventQuantity",
						dept
					)
				case MonitoringPointType.AssignmentSubmission =>
					validateTypeAssignmentSubmission(errors,
						point.assignmentSubmissionIsSpecificAssignments,
						point.assignmentSubmissionQuantity, "assignmentSubmissionQuantity",
						point.assignmentSubmissionModules.toSet.asJava, "assignmentSubmissionModules",
						point.assignmentSubmissionAssignments.toSet.asJava, "assignmentSubmissionAssignments",
						dept
					)
				case _ =>
			}

			if (monitoringPoints.asScala.count(p =>
				p.name == point.name && p.validFromWeek == point.validFromWeek && p.requiredFromWeek == point.requiredFromWeek
			) > 1) {
				errors.rejectValue("name", "monitoringPoint.name.exists")
			}
			errors.popNestedPath()
		}
	}
}

trait AddMonitoringPointSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AddMonitoringPointSetState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheckAny(
			Seq(CheckablePermission(Permissions.MonitoringPoints.Manage, mandatory(dept))) ++
			dept.routes.asScala.map { route => CheckablePermission(Permissions.MonitoringPoints.Manage, route) }
		)
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



trait AddMonitoringPointSetState extends GroupMonitoringPointsByTerm with CourseAndRouteServiceComponent with PermissionsAwareRoutes {

	private def getAvailableYears = {
		val routeMap = availableRoutes.map {
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
			r <- availableRoutes
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
	def user: CurrentUser

	def existingSetOption: Option[MonitoringPointSet]
	def template: Option[MonitoringPointSetTemplate]

	def academicYear: AcademicYear

	var changeYear = false

	lazy val availableRoutes = routesForPermission(user, Permissions.MonitoringPoints.Manage, dept).toSeq.sorted(Route.DegreeTypeOrdering)

	lazy val availableYears = getAvailableYears

	val pointSetToCopy = null

	val monitoringPoints = new AutoPopulatingList(classOf[MonitoringPoint])
	existingSetOption.foreach { set =>
		set.points.asScala.foreach(p => monitoringPoints.add(p))
	}
	template.foreach { template =>
		template.points.asScala.foreach(p => monitoringPoints.add(p.toPoint))
	}

	def monitoringPointsByTerm = groupByTerm(monitoringPoints.asScala, academicYear)

	lazy val selectedRoutesAndYears: java.util.Map[Route, java.util.HashMap[String, java.lang.Boolean]] = availableRoutes.map {
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
