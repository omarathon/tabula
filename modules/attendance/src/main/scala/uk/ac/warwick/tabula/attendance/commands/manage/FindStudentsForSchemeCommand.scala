package uk.ac.warwick.tabula.attendance.commands.manage

import org.hibernate.criterion.Order
import org.hibernate.criterion.Order._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.data.{SchemeMembershipExcludeType, SchemeMembershipIncludeType, SchemeMembershipItem, SchemeMembershipStaticType}
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringUserLookupComponent, ProfileServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

case class FindStudentsForSchemeCommandResult(
	staticStudentIds: JList[String],
	membershipItems: Seq[SchemeMembershipItem]
)

object FindStudentsForSchemeCommand {
	def apply(scheme: AttendanceMonitoringScheme, user: CurrentUser) =
		new FindStudentsForSchemeCommandInternal(scheme, user)
			with AutowiringProfileServiceComponent
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringDeserializesFilterImpl
			with AutowiringUserLookupComponent
			with ComposableCommand[FindStudentsForSchemeCommandResult]
			with PopulateFindStudentsForSchemeCommand
			with UpdatesFindStudentsForSchemeCommand
			with FindStudentsForSchemePermissions
			with FindStudentsForSchemeCommandState
			with Unaudited with ReadOnly
}


class FindStudentsForSchemeCommandInternal(val scheme: AttendanceMonitoringScheme, val user: CurrentUser)
	extends CommandInternal[FindStudentsForSchemeCommandResult] with TaskBenchmarking {

	self: ProfileServiceComponent with FindStudentsForSchemeCommandState with AttendanceMonitoringServiceComponent with UserLookupComponent =>

	override def applyInternal() = {
		if (serializeFilter.isEmpty) {
			FindStudentsForSchemeCommandResult(staticStudentIds, Seq())
		} else {
			staticStudentIds = benchmarkTask("profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments") {
				profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments(
					department = department,
					restrictions = buildRestrictions(),
					orders = buildOrders()
				).filter(userLookup.getUserByWarwickUniId(_).isFoundUser)
			}.asJava

			val startResult = studentsPerPage * (page-1)
			val staticMembershipItemsToDisplay = attendanceMonitoringService.findSchemeMembershipItems(
				staticStudentIds.asScala.slice(startResult, startResult + studentsPerPage),
				SchemeMembershipStaticType
			)

			val membershipItems: Seq[SchemeMembershipItem] = {
				staticMembershipItemsToDisplay.map{ item =>
					if (excludedStudentIds.asScala.contains(item.universityId))
						SchemeMembershipItem(SchemeMembershipExcludeType, item.firstName, item.lastName, item.universityId, item.userId, item.existingSchemes)
					else if (includedStudentIds.asScala.contains(item.universityId))
						SchemeMembershipItem(SchemeMembershipIncludeType, item.firstName, item.lastName, item.universityId, item.userId, item.existingSchemes)
					else
						item
				}
			}

			FindStudentsForSchemeCommandResult(staticStudentIds, membershipItems)
		}
	}

}

trait PopulateFindStudentsForSchemeCommand extends PopulateOnForm {

	self: FindStudentsForSchemeCommandState =>

	override def populate() = {
		staticStudentIds = scheme.members.staticUserIds.asJava
		includedStudentIds = scheme.members.includedUserIds.asJava
		excludedStudentIds = scheme.members.excludedUserIds.asJava
		filterQueryString = scheme.memberQuery
		linkToSits = scheme.memberQuery != null && scheme.memberQuery.nonEmpty
		// Default to current students
		if (filterQueryString == null || filterQueryString.size == 0)
			allSprStatuses.find(_.code == "C").map(sprStatuses.add)
		else
			deserializeFilter(filterQueryString)
	}

}

trait UpdatesFindStudentsForSchemeCommand {

	self: FindStudentsForSchemeCommandState =>

	def update(editSchemeMembershipCommandResult: EditSchemeMembershipCommandResult) = {
		includedStudentIds = editSchemeMembershipCommandResult.includedStudentIds
		excludedStudentIds = editSchemeMembershipCommandResult.excludedStudentIds
		// Default to current students
		if (filterQueryString == null || filterQueryString.size == 0)
			allSprStatuses.find(_.code == "C").map(sprStatuses.add)
		else
			deserializeFilter(filterQueryString)
	}

}

trait FindStudentsForSchemePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: FindStudentsForSchemeCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, scheme)
	}

}

trait FindStudentsForSchemeCommandState extends FiltersStudents with DeserializesFilter {
	def scheme: AttendanceMonitoringScheme
	def user: CurrentUser

	def department: Department = scheme.department

	// Bind variables

	var includedStudentIds: JList[String] = LazyLists.create()
	var excludedStudentIds: JList[String] = LazyLists.create()
	var staticStudentIds: JList[String] = LazyLists.create()
	var filterQueryString: String = ""
	var linkToSits: Boolean = _

	// Filter properties
	val defaultOrder = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm
	var sortOrder: JList[Order] = JArrayList()
	var page = 1
	def totalResults = staticStudentIds.size
	val studentsPerPage = FiltersStudents.DefaultStudentsPerPage

	// Filter binds
	var courseTypes: JList[CourseType] = JArrayList()
	var routes: JList[Route] = JArrayList()
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()
}
