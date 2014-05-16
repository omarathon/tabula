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
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringCourseAndRouteServiceComponent, AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent, AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.helpers.LazyLists
import collection.JavaConverters._
import uk.ac.warwick.tabula.data.{AutowiringSitsStatusDaoComponent, AutowiringModeOfAttendanceDaoComponent, SchemeMembershipIncludeType, SchemeMembershipExcludeType, SchemeMembershipStaticType, SchemeMembershipItem}

case class FindStudentsForSchemeCommandResult(
	updatedStaticStudentIds: JList[String],
	membershipItems: Seq[SchemeMembershipItem]
)

object FindStudentsForSchemeCommand {
	def apply(scheme: AttendanceMonitoringScheme, user: CurrentUser) =
		new FindStudentsForSchemeCommandInternal(scheme, user)
			with AutowiringSecurityServicePermissionsAwareRoutes
			with AutowiringProfileServiceComponent
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringCourseAndRouteServiceComponent
			with AutowiringModeOfAttendanceDaoComponent
			with AutowiringModuleAndDepartmentServiceComponent
			with AutowiringSitsStatusDaoComponent
			with ComposableCommand[FindStudentsForSchemeCommandResult]
			with PopulateFindStudentsForSchemeCommand
			with FindStudentsForSchemePermissions
			with FindStudentsForSchemeCommandState
			with Unaudited with ReadOnly
}


class FindStudentsForSchemeCommandInternal(val scheme: AttendanceMonitoringScheme, val user: CurrentUser)
	extends CommandInternal[FindStudentsForSchemeCommandResult] with TaskBenchmarking {

	self: ProfileServiceComponent with FindStudentsForSchemeCommandState with AttendanceMonitoringServiceComponent =>

	override def applyInternal() = {
		if (serializeFilter.isEmpty) {
			FindStudentsForSchemeCommandResult(updatedStaticStudentIds, Seq())
		} else {
			updatedStaticStudentIds = benchmarkTask("profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments") {
				profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments(
					department = department,
					restrictions = buildRestrictions(),
					orders = buildOrders()
				)
			}.asJava

			val startResult = studentsPerPage * (page-1)
			val staticMembershipItemsToDisplay = attendanceMonitoringService.findSchemeMembershipItems(
				updatedStaticStudentIds.asScala.slice(startResult, startResult + studentsPerPage),
				SchemeMembershipStaticType
			)

			val membershipItems: Seq[SchemeMembershipItem] = {
				staticMembershipItemsToDisplay.map{ item =>
					if (updatedExcludedStudentIds.asScala.contains(item.universityId))
						SchemeMembershipItem(SchemeMembershipExcludeType, item.firstName, item.lastName, item.universityId, item.userId)
					else if (updatedIncludedStudentIds.asScala.contains(item.universityId))
						SchemeMembershipItem(SchemeMembershipIncludeType, item.firstName, item.lastName, item.universityId, item.userId)
					else
						item
				}
			}

			FindStudentsForSchemeCommandResult(updatedStaticStudentIds, membershipItems)
		}
	}

}

trait PopulateFindStudentsForSchemeCommand extends PopulateOnForm {

	self: FindStudentsForSchemeCommandState =>

	override def populate() = {
		updatedIncludedStudentIds = includedStudentIds
		updatedExcludedStudentIds = excludedStudentIds
		updatedStaticStudentIds = staticStudentIds
		deserializeFilter(filterQueryString)
	}

}

trait FindStudentsForSchemePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: FindStudentsForSchemeCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, scheme)
	}

}

trait FindStudentsForSchemeCommandState extends PermissionsAwareRoutes with FiltersStudents {
	def scheme: AttendanceMonitoringScheme
	def user: CurrentUser

	def department: Department = scheme.department

	// Bind variables

	// Store original students for reset
	var includedStudentIds: JList[String] = LazyLists.create()
	var excludedStudentIds: JList[String] = LazyLists.create()
	var staticStudentIds: JList[String] = LazyLists.create()
	var filterQueryString: String = ""

	// Store updated students
	var updatedIncludedStudentIds: JList[String] = LazyLists.create()
	var updatedExcludedStudentIds: JList[String] = LazyLists.create()
	var updatedStaticStudentIds: JList[String] = LazyLists.create()
	var updatedFilterQueryString: String = ""

	// Filter properties
	val defaultOrder = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm
	var sortOrder: JList[Order] = JArrayList()
	var page = 1
	def totalResults = updatedStaticStudentIds.size
	val studentsPerPage = FiltersStudents.DefaultStudentsPerPage

	// Filter binds
	var courseTypes: JList[CourseType] = JArrayList()
	var routes: JList[Route] = JArrayList()
	lazy val visibleRoutes = routesForPermission(user, Permissions.MonitoringPoints.Manage, scheme.department)
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()
}
