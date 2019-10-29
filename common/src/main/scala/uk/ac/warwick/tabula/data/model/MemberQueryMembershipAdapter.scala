package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.groups.RemoveUserFromSmallGroupCommand
import uk.ac.warwick.tabula.commands.groups.admin.reusable.RemoveUserFromDepartmentSmallGroupCommand
import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroup, DepartmentSmallGroupSet, SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

sealed trait MemberQueryMembershipAdapter {
  def memberQuery: String

  def memberQuery_=(query: String): Unit

  def staticUserIds: Set[String]

  def staticUserIds_=(ids: Set[String]): Unit

  def includedUserIds: Set[String]

  def includedUserIds_=(ids: Set[String]): Unit

  def excludedUserIds: Set[String]

  def excludedUserIds_=(ids: Set[String]): Unit

  def users: Set[User]

  def finish(): Unit = {}
}

object MemberQueryMembershipAdapter {
  def apply(set: DepartmentSmallGroupSet): MemberQueryMembershipAdapter = new DepartmentSmallGroupSetMemberQueryMembershipAdapter(set)

  def apply(set: SmallGroupSet): MemberQueryMembershipAdapter = new SmallGroupSetMemberQueryMembershipAdapter(set)
}

class DepartmentSmallGroupSetMemberQueryMembershipAdapter(set: DepartmentSmallGroupSet) extends MemberQueryMembershipAdapter with AutoDeregistration[DepartmentSmallGroup] {
  override def memberQuery: String = set.memberQuery

  override def memberQuery_=(query: String): Unit = set.memberQuery = query

  private val userGroup: KnownTypeUserGroup = set.members.knownType

  override def staticUserIds: Set[String] = userGroup.staticUserIds

  override def staticUserIds_=(ids: Set[String]): Unit = userGroup.staticUserIds = ids

  override def includedUserIds: Set[String] = userGroup.includedUserIds

  override def includedUserIds_=(ids: Set[String]): Unit = userGroup.includedUserIds = ids

  override def excludedUserIds: Set[String] = userGroup.excludedUserIds

  override def excludedUserIds_=(ids: Set[String]): Unit = userGroup.excludedUserIds = ids

  override def users: Set[User] = set.members.users

  override def groups: Seq[DepartmentSmallGroup] = set.groups.asScala.toSeq

  override def userInGroup(user: User, group: DepartmentSmallGroup): Boolean = group.students.includesUser(user)

  override def removeFromGroup(user: User, group: DepartmentSmallGroup): Unit = new RemoveUserFromDepartmentSmallGroupCommand(user, group).apply()

  override def finish(): Unit = {
    if (set.department.autoGroupDeregistration) {
      deregister()
    }
  }
}

class SmallGroupSetMemberQueryMembershipAdapter(set: SmallGroupSet) extends MemberQueryMembershipAdapter with AutoDeregistration[SmallGroup] {
  override def memberQuery: String = set.memberQuery

  override def memberQuery_=(query: String): Unit = set.memberQuery = query

  private val userGroup: KnownTypeUserGroup = set.members.knownType

  override def staticUserIds: Set[String] = userGroup.staticUserIds

  override def staticUserIds_=(ids: Set[String]): Unit = userGroup.staticUserIds = ids

  override def includedUserIds: Set[String] = userGroup.includedUserIds

  override def includedUserIds_=(ids: Set[String]): Unit = userGroup.includedUserIds = ids

  override def excludedUserIds: Set[String] = userGroup.excludedUserIds

  override def excludedUserIds_=(ids: Set[String]): Unit = userGroup.excludedUserIds = ids

  override def users: Set[User] = set.members.users

  override def groups: Seq[SmallGroup] = set.groups.asScala.toSeq

  override def userInGroup(user: User, group: SmallGroup): Boolean = group.students.includesUser(user)

  override def removeFromGroup(user: User, group: SmallGroup): Unit = new RemoveUserFromSmallGroupCommand(user, group).apply()

  override def finish(): Unit = {
    if (set.department.autoGroupDeregistration) {
      deregister()
    }
  }
}

trait AutoDeregistration[A] {
  self: MemberQueryMembershipAdapter =>

  def groups: Seq[A]

  def userInGroup(user: User, group: A): Boolean

  def removeFromGroup(user: User, group: A): Unit

  private val oldUsers: Set[User] = users

  def deregister(): Unit = {
    val newUsers = users
    val removedUsers = oldUsers -- newUsers

    for {
      user <- removedUsers
      group <- groups if userInGroup(user, group)
    } removeFromGroup(user, group)
  }
}

object UpdateEntityMembershipByMemberQuery {
  def updateEntityMembership(state: UpdateEntityMembershipByMemberQueryCommandState, adapter: MemberQueryMembershipAdapter): Unit = {
    if (state.linkToSits) {
      adapter.staticUserIds = state.staticStudentIds.asScala.toSet
      adapter.includedUserIds = state.includedStudentIds.asScala.toSet
      adapter.excludedUserIds = state.excludedStudentIds.asScala.toSet
      adapter.memberQuery = state.filterQueryString
    } else {
      adapter.staticUserIds = Set.empty
      adapter.excludedUserIds = Set.empty
      adapter.memberQuery = null
      adapter.includedUserIds = ((state.staticStudentIds.asScala diff state.excludedStudentIds.asScala) ++ state.includedStudentIds.asScala).toSet
    }

    adapter.finish()
  }
}

trait UpdateEntityMembershipByMemberQueryCommandState {
  // Bind variables
  var includedStudentIds: JList[String] = LazyLists.create()
  var excludedStudentIds: JList[String] = LazyLists.create()
  var staticStudentIds: JList[String] = LazyLists.create()
  var filterQueryString: String = ""
  var linkToSits = true
}

