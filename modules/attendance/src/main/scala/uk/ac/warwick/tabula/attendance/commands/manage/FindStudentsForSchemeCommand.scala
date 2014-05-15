package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import org.hibernate.criterion.Order._
import uk.ac.warwick.tabula.JavaImports._
import org.hibernate.criterion.Order
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.attendance.commands.{AutowiringSecurityServicePermissionsAwareRoutes, PermissionsAwareRoutes}
import uk.ac.warwick.tabula.CurrentUser
import collection.JavaConverters._
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.util.web.UriBuilder

case class FindStudentsForSchemeCommandResult(
	mebershipItems: Seq[SchemeMembershipItem],
	totalResults: Int
)

object FindStudentsForSchemeCommand {
	def apply(scheme: AttendanceMonitoringScheme, user: CurrentUser) =
		new FindStudentsForSchemeCommandInternal(scheme, user)
			with AutowiringSecurityServicePermissionsAwareRoutes
			with AutowiringProfileServiceComponent
			with ComposableCommand[FindStudentsForSchemeCommandResult]
			with PopulateFindStudentsForSchemeCommand
			with FindStudentsForSchemePermissions
			with FindStudentsForSchemeCommandState
			with Unaudited with ReadOnly
}


class FindStudentsForSchemeCommandInternal(val scheme: AttendanceMonitoringScheme, val user: CurrentUser)
	extends CommandInternal[FindStudentsForSchemeCommandResult] {

	self: ProfileServiceComponent with FindStudentsForSchemeCommandState =>

	protected def modulesForDepartmentAndSubDepartments(department: Department): Seq[Module] =
		(department.modules.asScala ++ department.children.asScala.flatMap { modulesForDepartmentAndSubDepartments }).sorted

	protected def routesForDepartmentAndSubDepartments(department: Department): Seq[Route] =
		(department.routes.asScala ++ department.children.asScala.flatMap { routesForDepartmentAndSubDepartments }).sorted

	lazy val allModules: Seq[Module] = modulesForDepartmentAndSubDepartments(scheme.department)
	lazy val allCourseTypes: Seq[CourseType] = scheme.department.filterRule.courseTypes
	lazy val allRoutes: Seq[Route] = routesForDepartmentAndSubDepartments(scheme.department) match {
		case Nil => routesForDepartmentAndSubDepartments(scheme.department.rootDepartment).sorted(Route.DegreeTypeOrdering)
		case r => r.sorted(Route.DegreeTypeOrdering)
	}
	lazy val allSprStatuses: Seq[SitsStatus] = profileService.allSprStatuses(scheme.department.rootDepartment)
	lazy val allModesOfAttendance: Seq[ModeOfAttendance] = profileService.allModesOfAttendance(scheme.department.rootDepartment)
	val allYearsOfStudy: Seq[Int] = 1 to 8

	def serializeFilter = {
		val result = new UriBuilder()
		courseTypes.asScala.foreach(p => result.addQueryParameter("courseTypes", p.code))
		routes.asScala.foreach(p => result.addQueryParameter("routes", p.code))
		modesOfAttendance.asScala.foreach(p => result.addQueryParameter("modesOfAttendance", p.code))
		yearsOfStudy.asScala.foreach(p => result.addQueryParameter("yearsOfStudy", p.toString))
		sprStatuses.asScala.foreach(p => result.addQueryParameter("sprStatuses", p.code))
		modules.asScala.foreach(p => result.addQueryParameter("modules", p.code))
		if (result.getQuery == null)
			""
		else
			result.getQuery
	}

	override def applyInternal() = {


		FindStudentsForSchemeCommandResult(Seq(), 0)
	}

}

trait PopulateFindStudentsForSchemeCommand extends PopulateOnForm {

	self: FindStudentsForSchemeCommandState =>

	override def populate() = {
		updatedStaticStudentIds = staticStudentIds
	}

}

trait FindStudentsForSchemePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: FindStudentsForSchemeCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, scheme)
	}

}

trait FindStudentsForSchemeCommandState extends PermissionsAwareRoutes {
	def scheme: AttendanceMonitoringScheme
	def user: CurrentUser

	// Bind variables

	// Store original students for reset
	var staticStudentIds: JList[String] = LazyLists.create()
	var filterQueryString: String = ""

	// Store updated students
	var updatedStaticStudentIds: JList[String] = LazyLists.create()

	val defaultOrder = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm
	var sortOrder: JList[Order] = JArrayList()

	var courseTypes: JList[CourseType] = JArrayList()
	var routes: JList[Route] = JArrayList()
	lazy val visibleRoutes = routesForPermission(user, Permissions.MonitoringPoints.Manage, scheme.department)
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()
}
