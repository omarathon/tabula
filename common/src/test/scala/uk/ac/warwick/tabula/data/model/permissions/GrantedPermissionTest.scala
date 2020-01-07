package uk.ac.warwick.tabula.data.model.permissions

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{Fixtures, TestBase}
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}

class GrantedPermissionTest extends TestBase {

  val dept: Department = Fixtures.department("in")
  val module: Module = Fixtures.module("in101")
  val assignment: Assignment = Fixtures.assignment("assignment")
  val staffMember: StaffMember = Fixtures.staff()
  val studentMember: StudentMember = Fixtures.student()

  val feedback: Feedback = Fixtures.assignmentFeedback()

  val permission = Permissions.Module.Create
  val overrideType = true

  @Test def initDepartment: Unit = {
    GrantedPermission.canDefineFor[Department] should be(true)
    val gp = GrantedPermission(dept, permission, overrideType)
    gp.scope should be(dept)
    gp.permission should be(permission)
    gp.overrideType should be(overrideType)
  }

  @Test def initModule: Unit = {
    GrantedPermission.canDefineFor[Module] should be(true)
    val gp = GrantedPermission(module, permission, overrideType)
    gp.scope should be(module)
    gp.permission should be(permission)
    gp.overrideType should be(overrideType)
  }

  @Test def initAssignment: Unit = {
    GrantedPermission.canDefineFor[Assignment] should be(true)
    val gp = GrantedPermission(assignment, permission, overrideType)
    gp.scope should be(assignment)
    gp.permission should be(permission)
    gp.overrideType should be(overrideType)
  }

  @Test def initStaffMember: Unit = {
    GrantedPermission.canDefineFor[StaffMember] should be(true)
    val gp = GrantedPermission(staffMember, permission, overrideType)
    gp.scope should be(staffMember)
    gp.permission should be(permission)
    gp.overrideType should be(overrideType)
  }

  @Test def initStudentMember: Unit = {
    GrantedPermission.canDefineFor[StudentMember] should be(true)
    val gp = GrantedPermission(studentMember, permission, overrideType)
    gp.scope should be(studentMember)
    gp.permission should be(permission)
    gp.overrideType should be(overrideType)
  }

  @Test(expected = classOf[IllegalArgumentException]) def initInvalid: Unit = {
    GrantedPermission.canDefineFor[Feedback] should be(false)
    GrantedPermission(feedback, permission, overrideType)
  }

  @Test def scopeType(): Unit = {
    GrantedPermission.scopeType[Assignment] should be(Some("Assignment"))
    GrantedPermission.scopeType[PermissionsTarget] should be(None)
  }

}