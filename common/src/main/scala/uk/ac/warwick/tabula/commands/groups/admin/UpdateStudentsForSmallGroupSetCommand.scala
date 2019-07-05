package uk.ac.warwick.tabula.commands.groups.admin

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.UserGroupMembershipType._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, AutowiringUserLookupComponent, SmallGroupServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object UpdateStudentsForSmallGroupSetCommand {
  def apply(department: Department, module: Module, set: SmallGroupSet) =
    new UpdateStudentsForSmallGroupSetCommandInternal(department, module, set)
      with ComposableCommand[SmallGroupSet]
      with UpdateStudentsForDepartmentSmallGroupSetPermissions
      with UpdateStudentsForDepartmentSmallGroupSetDescription
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
  self: UserLookupComponent with SmallGroupServiceComponent =>

  override def applyInternal(): SmallGroupSet = {
    UpdateEntityMembershipByMemberQuery.updateEntityMembership(this, MemberQueryMembershipAdapter(set))

    smallGroupService.saveOrUpdate(set)

    set
  }
}

trait UpdateStudentsForSmallGroupSetCommandState extends UpdateEntityMembershipByMemberQueryCommandState {
  self: UserLookupComponent =>

  def department: Department

  def module: Module

  def set: SmallGroupSet

  def membershipItems: Seq[UserGroupMembershipItem] = {
    def toMembershipItem(universityId: String, itemType: UserGroupMembershipType) = {
      val user = userLookup.getUserByWarwickUniId(universityId)
      UserGroupMembershipItem(itemType, user.getFirstName, user.getLastName, user.getWarwickId, user.getUserId)
    }

    val staticMemberItems =
      ((staticStudentIds.asScala diff excludedStudentIds.asScala) diff includedStudentIds.asScala)
        .map(toMembershipItem(_, Static))

    val includedMemberItems = includedStudentIds.asScala.map(toMembershipItem(_, Include))

    (staticMemberItems ++ includedMemberItems).sortBy(membershipItem => (membershipItem.lastName, membershipItem.firstName))
  }
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

