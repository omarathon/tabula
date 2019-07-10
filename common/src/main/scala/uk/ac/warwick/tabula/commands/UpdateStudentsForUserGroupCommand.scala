package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.data.model.UserGroupMembershipType._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroupSet, SmallGroupSet}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object UpdateStudentsForUserGroupCommand {
  def apply(theDepartment: Department, theSet: DepartmentSmallGroupSet) =
    new UpdateStudentsForUserGroupCommandInternal[DepartmentSmallGroupSet](MemberQueryMembershipAdapter(theSet))
      with ComposableCommand[DepartmentSmallGroupSet]
      with UpdateStudentsForDepartmentSmallGroupSetPermissions
      with UpdateStudentsForDepartmentSmallGroupSetDescription
      with AutowiringUserLookupComponent
      with AutowiringSmallGroupServiceComponent {

      override def department: Department = theDepartment

      override def set: DepartmentSmallGroupSet = theSet

      override def save(): DepartmentSmallGroupSet = {
        smallGroupService.saveOrUpdate(set)

        set
      }
    }

  def apply(theDepartment: Department, theModule: Module, theSet: SmallGroupSet) =
    new UpdateStudentsForUserGroupCommandInternal[SmallGroupSet](MemberQueryMembershipAdapter(theSet))
      with ComposableCommand[SmallGroupSet]
      with UpdateStudentsForSmallGroupSetPermissions
      with UpdateStudentsForSmallGroupSetDescription
      with AutowiringUserLookupComponent
      with AutowiringSmallGroupServiceComponent {

      override def module: Module = theModule

      override def set: SmallGroupSet = theSet

      override def save(): SmallGroupSet = {
        smallGroupService.saveOrUpdate(set)

        set
      }
    }
}

trait UpdateStudentsForUserGroupCommandFactory {
  def apply(department: Department, module: Module, set: SmallGroupSet): Appliable[SmallGroupSet] with UpdateStudentsForUserGroupCommandState

  def apply(department: Department, set: DepartmentSmallGroupSet): Appliable[DepartmentSmallGroupSet] with UpdateStudentsForUserGroupCommandState
}

object UpdateStudentsForUserGroupCommandFactoryImpl
  extends UpdateStudentsForUserGroupCommandFactory {

  override def apply(department: Department, module: Module, set: SmallGroupSet) =
    UpdateStudentsForUserGroupCommand(department, module, set)

  override def apply(department: Department, set: DepartmentSmallGroupSet) =
    UpdateStudentsForUserGroupCommand(department, set)
}

abstract class UpdateStudentsForUserGroupCommandInternal[A](val adapter: MemberQueryMembershipAdapter)
  extends CommandInternal[A] with UpdateStudentsForUserGroupCommandState {
  self: UserLookupComponent =>

  override def applyInternal(): A = {
    UpdateEntityMembershipByMemberQuery.updateEntityMembership(this, adapter)

    save()
  }

  def save(): A
}

trait UpdateStudentsForUserGroupCommandState extends UpdateEntityMembershipByMemberQueryCommandState {
  self: UserLookupComponent =>

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

trait UpdateStudentsForSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: UpdateStudentsForUserGroupCommandState =>

  def set: SmallGroupSet

  def module: Module

  override def permissionsCheck(p: PermissionsChecking) {
    mustBeLinked(set, module)
    p.PermissionCheck(Permissions.SmallGroups.Update, set)
  }

}

trait UpdateStudentsForDepartmentSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  def set: DepartmentSmallGroupSet

  def department: Department

  override def permissionsCheck(p: PermissionsChecking) {
    mustBeLinked(set, department)
    p.PermissionCheck(Permissions.SmallGroups.Update, set)
  }

}

trait UpdateStudentsForDepartmentSmallGroupSetDescription extends Describable[DepartmentSmallGroupSet] {

  def set: DepartmentSmallGroupSet

  override lazy val eventName = "UpdateStudentsForDepartmentSmallGroupSet"

  override def describe(d: Description) {
    d.properties("smallGroupSet" -> set.id)
  }
}


trait UpdateStudentsForSmallGroupSetDescription extends Describable[SmallGroupSet] {

  def set: SmallGroupSet

  override lazy val eventName = "UpdateStudentsForSmallGroupSet"

  override def describe(d: Description) {
    d.properties("smallGroupSet" -> set.id)
  }
}

