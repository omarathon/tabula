package uk.ac.warwick.tabula.groups.commands.admin.reusable

import org.hibernate.criterion.Order
import org.hibernate.criterion.Order._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent, AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

case class FindStudentsForDepartmentSmallGroupSetCommandResult(
	updatedStaticStudentIds: JList[String],
	membershipItems: Seq[DepartmentSmallGroupSetMembershipItem]
)

object FindStudentsForDepartmentSmallGroupSetCommand {
	def apply(set: DepartmentSmallGroupSet) =
		new FindStudentsForDepartmentSmallGroupSetCommandInternal(set)
			with AutowiringProfileServiceComponent
			with AutowiringDeserializesFilterImpl
			with AutowiringUserLookupComponent
			with ComposableCommand[FindStudentsForDepartmentSmallGroupSetCommandResult]
			with PopulateFindStudentsForDepartmentSmallGroupSetCommand
			with UpdatesFindStudentsForDepartmentSmallGroupSetCommand
			with FindStudentsForDepartmentSmallGroupSetPermissions
			with FindStudentsForDepartmentSmallGroupSetCommandState
			with Unaudited with ReadOnly
}


class FindStudentsForDepartmentSmallGroupSetCommandInternal(val set: DepartmentSmallGroupSet)
	extends CommandInternal[FindStudentsForDepartmentSmallGroupSetCommandResult] with TaskBenchmarking {

	self: ProfileServiceComponent with FindStudentsForDepartmentSmallGroupSetCommandState with UserLookupComponent =>

	override def applyInternal() = {
		if (serializeFilter.isEmpty) {
			FindStudentsForDepartmentSmallGroupSetCommandResult(updatedStaticStudentIds, Seq())
		} else {
			updatedStaticStudentIds = benchmarkTask("profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments") {
				profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments(
					department = department,
					restrictions = buildRestrictions(),
					orders = buildOrders()
				).filter(userLookup.getUserByWarwickUniId(_).isFoundUser)
			}.asJava

			def toMembershipItem(universityId: String, itemType: DepartmentSmallGroupSetMembershipItemType) = {
				val user = userLookup.getUserByWarwickUniId(universityId)
				DepartmentSmallGroupSetMembershipItem(itemType, user.getFirstName, user.getLastName, user.getWarwickId, user.getUserId)
			}

			val startResult = studentsPerPage * (page-1)
			val staticMembershipItemsToDisplay =
				updatedStaticStudentIds.asScala.slice(startResult, startResult + studentsPerPage).map(toMembershipItem(_, DepartmentSmallGroupSetMembershipStaticType))

			val membershipItems: Seq[DepartmentSmallGroupSetMembershipItem] = {
				staticMembershipItemsToDisplay.map { item =>
					if (updatedExcludedStudentIds.asScala.contains(item.universityId))
						DepartmentSmallGroupSetMembershipItem(DepartmentSmallGroupSetMembershipExcludeType, item.firstName, item.lastName, item.universityId, item.userId)
					else if (updatedIncludedStudentIds.asScala.contains(item.universityId))
						DepartmentSmallGroupSetMembershipItem(DepartmentSmallGroupSetMembershipIncludeType, item.firstName, item.lastName, item.universityId, item.userId)
					else
						item
				}
			}

			FindStudentsForDepartmentSmallGroupSetCommandResult(updatedStaticStudentIds, membershipItems)
		}
	}

}

trait PopulateFindStudentsForDepartmentSmallGroupSetCommand extends PopulateOnForm {

	self: FindStudentsForDepartmentSmallGroupSetCommandState =>

	override def populate() = {
		updatedIncludedStudentIds = includedStudentIds
		updatedExcludedStudentIds = excludedStudentIds
		updatedStaticStudentIds = staticStudentIds
		deserializeFilter(filterQueryString)
	}

}

trait UpdatesFindStudentsForDepartmentSmallGroupSetCommand {

	self: FindStudentsForDepartmentSmallGroupSetCommandState =>

	def update(editSchemeMembershipCommandResult: EditDepartmentSmallGroupSetMembershipCommandResult) = {
		updatedIncludedStudentIds = editSchemeMembershipCommandResult.updatedIncludedStudentIds
		updatedExcludedStudentIds = editSchemeMembershipCommandResult.updatedExcludedStudentIds
		deserializeFilter(updatedFilterQueryString)
	}

}

trait FindStudentsForDepartmentSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: FindStudentsForDepartmentSmallGroupSetCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
	}
}

trait FindStudentsForDepartmentSmallGroupSetCommandState extends FiltersStudents with DeserializesFilter {
	def set: DepartmentSmallGroupSet

	def department: Department = set.department

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
	lazy val visibleRoutes = set.department.routes.asScala
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()
}
