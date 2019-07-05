package uk.ac.warwick.tabula.commands.groups.admin

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.UserGroupMembershipType._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.data.model.{Department, Module, UserGroupMembershipItem, UserGroupMembershipType}
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

case class AddUsersToEditSmallGroupSetSitsMembershipCommandResult(
  missingUsers: Seq[String]
)

object EditSmallGroupSetSitsMembershipCommand {
  def apply(department: Department, module: Module, set: SmallGroupSet) =
    new EditSmallGroupSetSitsMembershipCommandInternal(department, module, set)
      with AutowiringUserLookupComponent
      with ComposableCommand[EditUserGroupMembershipCommandResult]
      with PopulateEditSmallGroupSetSitsMembershipCommand
      with AddsUsersToEditSmallGroupSetSitsMembershipCommand
      with RemovesUsersFromEditSmallGroupSetSitsMembershipCommand
      with ResetsMembershipInEditSmallGroupSetSitsMembershipCommand
      with EditSmallGroupSetSitsMembershipPermissions
      with Unaudited with ReadOnly
}

/**
  * Not persisted, just used to validate users entered and render student table
  */
class EditSmallGroupSetSitsMembershipCommandInternal(val department: Department, val module: Module, val set: SmallGroupSet)
  extends CommandInternal[EditUserGroupMembershipCommandResult] with EditSmallGroupSetSitsMembershipCommandState {
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

trait PopulateEditSmallGroupSetSitsMembershipCommand extends PopulateOnForm {

  self: EditSmallGroupSetSitsMembershipCommandState =>

  override def populate(): Unit = {
    includedStudentIds = set.members.knownType.includedUserIds.toSeq.asJava
    excludedStudentIds = set.members.knownType.excludedUserIds.toSeq.asJava
  }

}

trait AddsUsersToEditSmallGroupSetSitsMembershipCommand {
  self: EditSmallGroupSetSitsMembershipCommandState with UserLookupComponent =>

  def addUsers(): AddUsersToEditSmallGroupSetSitsMembershipCommandResult = {
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

    val missingUsers = massAddedUserMap.filter(!_._2.isDefined).keys.toSeq
    val validUsers = massAddedUserMap.filter(_._2.isDefined).values.flatten.toSeq

    includedStudentIds = (includedStudentIds.asScala.toSeq ++ validUsers.map(_.getWarwickId)).distinct.asJava
    excludedStudentIds = (excludedStudentIds.asScala.toSeq diff includedStudentIds.asScala.toSeq).distinct.asJava

    // Users processed, so reset fields
    massAddUsers = ""

    AddUsersToEditSmallGroupSetSitsMembershipCommandResult(missingUsers)
  }

}

trait RemovesUsersFromEditSmallGroupSetSitsMembershipCommand {

  self: EditSmallGroupSetSitsMembershipCommandState =>

  def removeUsers(): Unit = {
    excludedStudentIds = (excludedStudentIds.asScala ++ excludeIds.asScala).distinct.asJava
  }
}

trait ResetsMembershipInEditSmallGroupSetSitsMembershipCommand {

  self: EditSmallGroupSetSitsMembershipCommandState =>

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


trait EditSmallGroupSetSitsMembershipPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: EditSmallGroupSetSitsMembershipCommandState =>

  override def permissionsCheck(p: PermissionsChecking) {
    mustBeLinked(set, module)
    p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
  }

}

trait EditSmallGroupSetSitsMembershipCommandState {
  def set: SmallGroupSet

  def department: Department

  def module: Module

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
