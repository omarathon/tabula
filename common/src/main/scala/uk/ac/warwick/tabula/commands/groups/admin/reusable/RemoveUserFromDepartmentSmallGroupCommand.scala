package uk.ac.warwick.tabula.commands.groups.admin.reusable

import uk.ac.warwick.tabula.commands.{Command, Description}
import uk.ac.warwick.tabula.data.model.UnspecifiedTypeUserGroup
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroup
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.userlookup.User

class RemoveUserFromDepartmentSmallGroupCommand(val user: User, val group: DepartmentSmallGroup) extends Command[UnspecifiedTypeUserGroup] {

  PermissionCheck(Permissions.SmallGroups.Update, group)

  def applyInternal(): UnspecifiedTypeUserGroup = {
    val ug = group.students
    ug.remove(user)
    ug
  }

  override def describe(d: Description): Unit =
    d.departmentSmallGroup(group)
     .studentUsercodes(user.getUserId)
     .studentIds(user.getWarwickId)

}
