package uk.ac.warwick.tabula.commands.groups.admin.reusable

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.UserGroupMembershipType._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, AutowiringUserLookupComponent, SmallGroupServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object UpdateStudentsForDepartmentSmallGroupSetCommand {
  def apply(department: Department, set: DepartmentSmallGroupSet) =
    new UpdateStudentsForDepartmentSmallGroupSetCommandInternal(department, set)
      with ComposableCommand[DepartmentSmallGroupSet]
      with UpdateStudentsForDepartmentSmallGroupSetPermissions
      with UpdateStudentsForDepartmentSmallGroupSetDescription
      with AutowiringUserLookupComponent
      with AutowiringSmallGroupServiceComponent
}

trait UpdateStudentsForDepartmentSmallGroupSetCommandFactory {
  def apply(department: Department, set: DepartmentSmallGroupSet): Appliable[DepartmentSmallGroupSet] with UpdateStudentsForDepartmentSmallGroupSetCommandState
}

object UpdateStudentsForDepartmentSmallGroupSetCommandFactoryImpl
  extends UpdateStudentsForDepartmentSmallGroupSetCommandFactory {

  def apply(department: Department, set: DepartmentSmallGroupSet) =
    UpdateStudentsForDepartmentSmallGroupSetCommand(department, set)
}

class UpdateStudentsForDepartmentSmallGroupSetCommandInternal(val department: Department, val set: DepartmentSmallGroupSet)
  extends CommandInternal[DepartmentSmallGroupSet] with UpdateStudentsForDepartmentSmallGroupSetCommandState {
  self: UserLookupComponent with SmallGroupServiceComponent =>

  override def applyInternal(): DepartmentSmallGroupSet = {
    UpdateEntityMembershipByMemberQuery.updateEntityMembership(this, MemberQueryMembershipAdapter(set))

    smallGroupService.saveOrUpdate(set)

    set
  }
}

trait UpdateStudentsForDepartmentSmallGroupSetCommandState extends UpdateEntityMembershipByMemberQueryCommandState {
  self: UserLookupComponent =>

  def department: Department

  def set: DepartmentSmallGroupSet

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
  self: UpdateStudentsForDepartmentSmallGroupSetCommandState =>

  override def permissionsCheck(p: PermissionsChecking) {
    mustBeLinked(set, department)
    p.PermissionCheck(Permissions.SmallGroups.Update, set)
  }

}

trait UpdateStudentsForDepartmentSmallGroupSetDescription extends Describable[DepartmentSmallGroupSet] {
  self: UpdateStudentsForDepartmentSmallGroupSetCommandState =>

  override lazy val eventName = "UpdateStudentsForDepartmentSmallGroupSet"

  override def describe(d: Description) {
    d.properties("smallGroupSet" -> set.id)
  }
}
