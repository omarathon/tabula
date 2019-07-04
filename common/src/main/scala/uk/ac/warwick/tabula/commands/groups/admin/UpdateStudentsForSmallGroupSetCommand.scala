package uk.ac.warwick.tabula.commands.groups.admin

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.RemoveUserFromSmallGroupCommand
import uk.ac.warwick.tabula.commands.groups.admin.reusable.{DepartmentSmallGroupSetMembershipIncludeType, DepartmentSmallGroupSetMembershipItem, DepartmentSmallGroupSetMembershipItemType, DepartmentSmallGroupSetMembershipStaticType}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, AutowiringUserLookupComponent, SmallGroupServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object UpdateStudentsForSmallGroupSetCommand {
  def apply(department: Department, module: Module, set: SmallGroupSet) =
    new UpdateStudentsForSmallGroupSetCommandInternal(department, module, set)
      with ComposableCommand[SmallGroupSet]
      with UpdateStudentsForDepartmentSmallGroupSetPermissions
      with UpdateStudentsForDepartmentSmallGroupSetDescription
      with RemovesUsersFromDepartmentGroupsCommand
      with AutowiringUserLookupComponent
      with AutowiringSmallGroupServiceComponent
}

trait UpdateStudentsForSmallGroupSetCommandFactory {
  def apply(department: Department, module: Module, set: SmallGroupSet): Appliable[SmallGroupSet] with UpdateStudentsForSmallGroupSetCommandState
}

object UpdateStudentsForSmallGroupSetCommandFactoryImpl
  extends UpdateStudentsForSmallGroupSetCommandFactory {

  def apply(department: Department, module: Module, set: SmallGroupSet) =
    UpdateStudentsForSmallGroupSetCommand(department, module, set)
}

class UpdateStudentsForSmallGroupSetCommandInternal(val department: Department, val module: Module, val set: SmallGroupSet)
  extends CommandInternal[SmallGroupSet] with UpdateStudentsForSmallGroupSetCommandState {
  self: UserLookupComponent with SmallGroupServiceComponent with RemovesUsersFromDepartmentGroups =>

  override def applyInternal(): SmallGroupSet = {
    val autoDeregister = module.adminDepartment.autoGroupDeregistration

    val oldUsers =
      if (autoDeregister) set.members.users.toSet
      else Set[User]()

    if (linkToSits) {
      set.members.knownType.staticUserIds = staticStudentIds.asScala.toSet
      set.members.knownType.includedUserIds = includedStudentIds.asScala.toSet
      set.members.knownType.excludedUserIds = excludedStudentIds.asScala.toSet
      set.memberQuery = filterQueryString
    } else {
      set.members.knownType.staticUserIds = Set.empty
      set.members.knownType.excludedUserIds = Set.empty
      set.memberQuery = null
      set.members.knownType.includedUserIds = ((staticStudentIds.asScala diff excludedStudentIds.asScala) ++ includedStudentIds.asScala).toSet
    }

    val newUsers =
      if (autoDeregister) set.members.users
      else Set[User]()

    // TAB-1561
    if (autoDeregister) {
      for {
        user <- oldUsers -- newUsers
        group <- set.groups.asScala
        if group.students.includesUser(user)
      } removeFromGroup(user, group)
    }

    smallGroupService.saveOrUpdate(set)

    set
  }
}

trait UpdateStudentsForSmallGroupSetCommandState {
  self: UserLookupComponent =>

  def department: Department

  def module: Module

  def set: SmallGroupSet

  def membershipItems: Seq[DepartmentSmallGroupSetMembershipItem] = {
    def toMembershipItem(universityId: String, itemType: DepartmentSmallGroupSetMembershipItemType) = {
      val user = userLookup.getUserByWarwickUniId(universityId)
      DepartmentSmallGroupSetMembershipItem(itemType, user.getFirstName, user.getLastName, user.getWarwickId, user.getUserId)
    }

    val staticMemberItems =
      ((staticStudentIds.asScala diff excludedStudentIds.asScala) diff includedStudentIds.asScala)
        .map(toMembershipItem(_, DepartmentSmallGroupSetMembershipStaticType))

    val includedMemberItems = includedStudentIds.asScala.map(toMembershipItem(_, DepartmentSmallGroupSetMembershipIncludeType))

    (staticMemberItems ++ includedMemberItems).sortBy(membershipItem => (membershipItem.lastName, membershipItem.firstName))
  }

  // Bind variables

  var includedStudentIds: JList[String] = LazyLists.create()
  var excludedStudentIds: JList[String] = LazyLists.create()
  var staticStudentIds: JList[String] = LazyLists.create()
  var filterQueryString: String = ""
  var linkToSits = true
}

trait UpdateStudentsForDepartmentSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: UpdateStudentsForSmallGroupSetCommandState =>

  override def permissionsCheck(p: PermissionsChecking) {
    mustBeLinked(set, module)
    p.PermissionCheck(Permissions.SmallGroups.Update, set)
  }

}

trait UpdateStudentsForDepartmentSmallGroupSetDescription extends Describable[SmallGroupSet] {
  self: UpdateStudentsForSmallGroupSetCommandState =>

  override lazy val eventName = "UpdateStudentsForDepartmentSmallGroupSet"

  override def describe(d: Description) {
    d.properties("smallGroupSet" -> set.id)
  }
}

trait RemovesUsersFromDepartmentGroups {
  def removeFromGroup(user: User, group: SmallGroup)
}

trait RemovesUsersFromDepartmentGroupsCommand extends RemovesUsersFromDepartmentGroups {
  def removeFromGroup(user: User, group: SmallGroup): Unit = new RemoveUserFromSmallGroupCommand(user, group).apply()
}