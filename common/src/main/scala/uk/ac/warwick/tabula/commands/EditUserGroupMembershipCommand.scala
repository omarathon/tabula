package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.tabula.data.model.UserGroupMembershipType._
import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroupSet, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.{Department, MemberQueryMembershipAdapter, Module, UserGroupMembershipItem, UserGroupMembershipType}
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

case class AddUsersToEditUserGroupMembershipCommandResult(
  missingUsers: Seq[String]
)

object EditUserGroupMembershipCommand {
  def apply(theDepartment: Department, theSet: DepartmentSmallGroupSet) =
    new EditUserGroupMembershipCommandInternal(MemberQueryMembershipAdapter(theSet))
      with AutowiringUserLookupComponent
      with ComposableCommand[EditUserGroupMembershipCommandResult]
      with PopulateEditUserGroupMembershipCommand
      with AddsUsersToEditUserGroupMembershipCommand
      with RemovesUsersFromEditUserGroupMembershipCommand
      with ResetsMembershipInEditUserGroupMembershipCommand
      with EditDepartmentSmallGroupSetMembershipPermissions
      with Unaudited with ReadOnly {
      override def department: Department = theDepartment

      override def set: DepartmentSmallGroupSet = theSet
    }

  def apply(theModule: Module, theSet: SmallGroupSet) =
    new EditUserGroupMembershipCommandInternal(MemberQueryMembershipAdapter(theSet))
      with AutowiringUserLookupComponent
      with ComposableCommand[EditUserGroupMembershipCommandResult]
      with PopulateEditUserGroupMembershipCommand
      with AddsUsersToEditUserGroupMembershipCommand
      with RemovesUsersFromEditUserGroupMembershipCommand
      with ResetsMembershipInEditUserGroupMembershipCommand
      with EditSmallGroupSetMembershipPermissions
      with Unaudited with ReadOnly {
      override def module: Module = theModule

      override def set: SmallGroupSet = theSet
    }
}

/**
  * Not persisted, just used to validate users entered and render student table
  */
class EditUserGroupMembershipCommandInternal(val adapter: MemberQueryMembershipAdapter)
  extends CommandInternal[EditUserGroupMembershipCommandResult] with EditUserGroupMembershipCommandState {
  self: UserLookupComponent =>

  override def applyInternal(): EditUserGroupMembershipCommandResult = {
    def toMembershipItem(universityId: String, itemType: UserGroupMembershipType) = {
      val user = userLookup.getUserByWarwickUniId(universityId)
      UserGroupMembershipItem(itemType, user.getFirstName, user.getLastName, user.getWarwickId, user.getUserId)
    }

    val membershipItems: Seq[UserGroupMembershipItem] = {
      val excludedMemberItems = excludedStudentIds.asScala.map(toMembershipItem(_, Exclude))
      val includedMemberItems = includedStudentIds.asScala.map(toMembershipItem(_, Include))
      (excludedMemberItems ++ includedMemberItems).sortBy(membershipItem => (membershipItem.lastName, membershipItem.firstName))
    }

    EditUserGroupMembershipCommandResult(
      includedStudentIds,
      excludedStudentIds,
      membershipItems
    )
  }

}

trait PopulateEditUserGroupMembershipCommand extends PopulateOnForm {

  self: EditUserGroupMembershipCommandState =>

  override def populate(): Unit = {
    includedStudentIds = adapter.includedUserIds.toSeq.asJava
    excludedStudentIds = adapter.excludedUserIds.toSeq.asJava
  }

}

trait AddsUsersToEditUserGroupMembershipCommand {
  self: EditUserGroupMembershipCommandState with UserLookupComponent =>

  def addUsers(): AddUsersToEditUserGroupMembershipCommandResult = {
    def getUserForString(entry: String): Option[User] = {
      if (UniversityId.isValid(entry)) {
        val user = userLookup.getUserByWarwickUniId(entry)

        if (user.isFoundUser) Some(user)
        else None
      } else {
        val user = userLookup.getUserByUserId(entry)

        if (user.isFoundUser) Some(user)
        else None
      }
    }

    val massAddedUserMap: Map[String, Option[User]] = massAddUsersEntries.map { entry =>
      entry -> getUserForString(entry)
    }.toMap

    val missingUsers = massAddedUserMap.filter(_._2.isEmpty).keys.toSeq
    val validUsers = massAddedUserMap.filter(_._2.isDefined).values.flatten.toSeq

    includedStudentIds = (includedStudentIds.asScala ++ validUsers.map(_.getWarwickId)).distinct.asJava
    excludedStudentIds = (excludedStudentIds.asScala diff includedStudentIds.asScala).distinct.asJava

    // Users processed, so reset fields
    massAddUsers = ""

    AddUsersToEditUserGroupMembershipCommandResult(missingUsers)
  }

}

trait RemovesUsersFromEditUserGroupMembershipCommand {

  self: EditUserGroupMembershipCommandState =>

  def removeUsers(): Unit = {
    excludedStudentIds = (excludedStudentIds.asScala ++ excludeIds.asScala).distinct.asJava
  }
}

trait ResetsMembershipInEditUserGroupMembershipCommand {

  self: EditUserGroupMembershipCommandState =>

  def resetMembership(): Unit = {
    includedStudentIds = (includedStudentIds.asScala diff resetStudentIds.asScala).asJava
    excludedStudentIds = (excludedStudentIds.asScala diff resetStudentIds.asScala).asJava
  }

  def resetAllIncluded(): Unit = {
    includedStudentIds.clear()
  }

  def resetAllExcluded(): Unit = {
    excludedStudentIds.clear()
  }
}


trait EditDepartmentSmallGroupSetMembershipPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  def set: DepartmentSmallGroupSet

  def department: Department

  override def permissionsCheck(p: PermissionsChecking) {
    mustBeLinked(set, department)
    p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
  }

}

trait EditSmallGroupSetMembershipPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  def set: SmallGroupSet

  def module: Module

  override def permissionsCheck(p: PermissionsChecking) {
    mustBeLinked(set, module)
    p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
  }
}

trait EditUserGroupMembershipCommandState {
  def adapter: MemberQueryMembershipAdapter

  // Bind variables

  var includedStudentIds: JList[String] = LazyLists.create()
  var excludedStudentIds: JList[String] = LazyLists.create()
  var staticStudentIds: JList[String] = LazyLists.create()

  var massAddUsers: String = _

  // parse massAddUsers into a collection of individual tokens
  def massAddUsersEntries: Seq[String] =
    if (massAddUsers == null) Nil
    else massAddUsers split "(\\s|[^A-Za-z\\d\\-_\\.])+" map (_.trim) filterNot (_.isEmpty)

  var excludeIds: JList[String] = LazyLists.create()
  var resetStudentIds: JList[String] = LazyLists.create()
}
