package uk.ac.warwick.tabula.commands.groups.admin

import org.hibernate.criterion.Order
import org.hibernate.criterion.Order._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.admin.reusable.{DepartmentSmallGroupSetMembershipExcludeType, DepartmentSmallGroupSetMembershipIncludeType, DepartmentSmallGroupSetMembershipItem, DepartmentSmallGroupSetMembershipItemType, DepartmentSmallGroupSetMembershipStaticType}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringUserLookupComponent, ProfileServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

case class FindStudentsForSmallGroupSetCommandResult(
  staticStudentIds: JList[String],
  membershipItems: Seq[DepartmentSmallGroupSetMembershipItem]
)

object FindStudentsForSmallGroupSetCommand {
  def apply(department: Department, module: Module, set: SmallGroupSet) =
    new FindStudentsForSmallGroupSetCommandInternal(department, module, set)
      with AutowiringProfileServiceComponent
      with AutowiringDeserializesFilterImpl
      with AutowiringUserLookupComponent
      with ComposableCommand[FindStudentsForSmallGroupSetCommandResult]
      with PopulateFindStudentsForSmallGroupSetCommand
      with UpdatesFindStudentsForSmallGroupSetCommand
      with FindStudentsForSmallGroupSetPermissions
      with FindStudentsForSmallGroupSetCommandState
      with FiltersStudents
      with DeserializesFilter
      with Unaudited with ReadOnly
}

trait FindStudentsForSmallGroupSetCommandFactory {
  def apply(department: Department, module: Module, set: SmallGroupSet): Appliable[FindStudentsForSmallGroupSetCommandResult]
    with FindStudentsForSmallGroupSetCommandState
    with PopulateOnForm
}

object FindStudentsForSmallGroupSetCommandFactoryImpl
  extends FindStudentsForSmallGroupSetCommandFactory {

  def apply(department: Department, module: Module, set: SmallGroupSet) =
    FindStudentsForSmallGroupSetCommand(department, module, set)
}

class FindStudentsForSmallGroupSetCommandInternal(val department: Department, val module: Module, val set: SmallGroupSet)
  extends CommandInternal[FindStudentsForSmallGroupSetCommandResult]
    with FindStudentsForSmallGroupSetCommandState
    with TaskBenchmarking {

  self: FiltersStudents with ProfileServiceComponent with UserLookupComponent =>

  override def applyInternal(): FindStudentsForSmallGroupSetCommandResult = {
    if (!doFind && (serializeFilter.isEmpty || findStudents.isEmpty)) {
      FindStudentsForSmallGroupSetCommandResult(staticStudentIds, Seq())
    } else {
      doFind = true
      val year = set.academicYear

      staticStudentIds = benchmarkTask("profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments") {
        profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments(
          department = department,
          restrictions = buildRestrictions(year),
          orders = buildOrders()
        ).filter(userLookup.getUserByWarwickUniId(_).isFoundUser)
      }.asJava

      def toMembershipItem(universityId: String, itemType: DepartmentSmallGroupSetMembershipItemType) = {
        val user = userLookup.getUserByWarwickUniId(universityId)
        DepartmentSmallGroupSetMembershipItem(itemType, user.getFirstName, user.getLastName, user.getWarwickId, user.getUserId)
      }

      val startResult = studentsPerPage * (page - 1)
      val staticMembershipItemsToDisplay =
        staticStudentIds.asScala.slice(startResult, startResult + studentsPerPage).map(toMembershipItem(_, DepartmentSmallGroupSetMembershipStaticType))

      val membershipItems: Seq[DepartmentSmallGroupSetMembershipItem] = {
        staticMembershipItemsToDisplay.map { item =>
          if (excludedStudentIds.asScala.contains(item.universityId))
            DepartmentSmallGroupSetMembershipItem(DepartmentSmallGroupSetMembershipExcludeType, item.firstName, item.lastName, item.universityId, item.userId)
          else if (includedStudentIds.asScala.contains(item.universityId))
            DepartmentSmallGroupSetMembershipItem(DepartmentSmallGroupSetMembershipIncludeType, item.firstName, item.lastName, item.universityId, item.userId)
          else
            item
        }
      }

      FindStudentsForSmallGroupSetCommandResult(staticStudentIds, membershipItems)
    }
  }

}

trait PopulateFindStudentsForSmallGroupSetCommand extends PopulateOnForm {

  self: FindStudentsForSmallGroupSetCommandState with FiltersStudents with DeserializesFilter =>

  override def populate(): Unit = {
    staticStudentIds = set.members.knownType.staticUserIds.toSeq.asJava
    includedStudentIds = set.members.knownType.includedUserIds.toSeq.asJava
    excludedStudentIds = set.members.knownType.excludedUserIds.toSeq.asJava
    filterQueryString = Option(set.memberQuery).getOrElse("")
    linkToSits = set.members.isEmpty || (set.memberQuery != null && set.memberQuery.nonEmpty)

    // Should only be true when editing
    if (linkToSits && set.members.knownType.members.nonEmpty) {
      doFind = true
    }

    // Default to current students
    if (!filterQueryString.hasText)
      allSprStatuses.find(_.code == "C").map(sprStatuses.add)
    else
      deserializeFilter(filterQueryString)
  }

}

trait UpdatesFindStudentsForSmallGroupSetCommand {

  self: FindStudentsForSmallGroupSetCommandState with FiltersStudents with DeserializesFilter =>

  def update(editSchemeMembershipCommandResult: EditSmallGroupSetSitsMembershipCommandResult): Any = {
    includedStudentIds = editSchemeMembershipCommandResult.includedStudentIds
    excludedStudentIds = editSchemeMembershipCommandResult.excludedStudentIds
    // Default to current students
    if (!filterQueryString.hasText)
      allSprStatuses.find(_.code == "C").map(sprStatuses.add)
    else
      deserializeFilter(filterQueryString)
  }

}

trait FindStudentsForSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: FindStudentsForSmallGroupSetCommandState =>

  override def permissionsCheck(p: PermissionsChecking) {
    mustBeLinked(set, module)
    p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
  }
}

trait FindStudentsForSmallGroupSetCommandState {
  def department: Department

  def module: Module

  def set: SmallGroupSet

  // Bind variables

  // Store original students for reset
  var includedStudentIds: JList[String] = LazyLists.create()
  var excludedStudentIds: JList[String] = LazyLists.create()
  var staticStudentIds: JList[String] = LazyLists.create()
  var filterQueryString: String = ""
  var linkToSits = true
  var findStudents: String = ""
  var doFind: Boolean = false

  // Filter properties
  val defaultOrder = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm
  var sortOrder: JList[Order] = JArrayList()
  var page = 1

  def totalResults: Int = staticStudentIds.size

  val studentsPerPage = FiltersStudents.DefaultStudentsPerPage

  // Filter binds
  var courseTypes: JList[CourseType] = JArrayList()
  var routes: JList[Route] = JArrayList()
  var courses: JList[Course] = JArrayList()
  var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
  var yearsOfStudy: JList[JInteger] = JArrayList()
  var levelCodes: JList[String] = JArrayList()
  var sprStatuses: JList[SitsStatus] = JArrayList()
  var modules: JList[Module] = JArrayList()
  var hallsOfResidence: JList[String] = JArrayList()
}
