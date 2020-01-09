package uk.ac.warwick.tabula.data.model.permissions

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{Fixtures, TestBase}
import uk.ac.warwick.tabula.roles.AssignmentSubmitterRoleDefinition
import uk.ac.warwick.tabula.permissions.Permissions

class GrantedRoleTest extends TestBase {

  val dept: Department = Fixtures.department("in")
  val module: Module = Fixtures.module("in101")
  module.adminDepartment = dept

  val assignment: Assignment = Fixtures.assignment("assignment")
  val staffMember: StaffMember = Fixtures.staff()
  val studentMember: StudentMember = Fixtures.student()

  val feedback: Feedback = Fixtures.assignmentFeedback()

  val roleDefinition = AssignmentSubmitterRoleDefinition

  @Test def initDepartment: Unit = {
    GrantedRole.canDefineFor[Department] should be(true)
    val gr = GrantedRole(dept, roleDefinition)
    gr.scope should be(dept)
    gr.roleDefinition should be(roleDefinition)
  }

  @Test def initModule: Unit = {
    GrantedRole.canDefineFor[Module] should be(true)
    val gr = GrantedRole(module, roleDefinition)
    gr.scope should be(module)
    gr.roleDefinition should be(roleDefinition)
  }

  @Test def initAssignment: Unit = {
    GrantedRole.canDefineFor[Assignment] should be(true)
    val gr = GrantedRole(assignment, roleDefinition)
    gr.scope should be(assignment)
    gr.roleDefinition should be(roleDefinition)
  }

  @Test def initStaffMember: Unit = {
    GrantedRole.canDefineFor[StaffMember] should be(true)
    val gr = GrantedRole(staffMember, roleDefinition)
    gr.scope should be(staffMember)
    gr.roleDefinition should be(roleDefinition)
  }

  @Test def initStudentMember: Unit = {
    GrantedRole.canDefineFor[StudentMember] should be(true)
    val gr = GrantedRole(studentMember, roleDefinition)
    gr.scope should be(studentMember)
    gr.roleDefinition should be(roleDefinition)
  }

  @Test(expected = classOf[IllegalArgumentException]) def initInvalid: Unit = {
    GrantedRole.canDefineFor[Feedback] should be(false)
    GrantedRole(feedback, roleDefinition)
  }

  @Test def build: Unit = {
    val gr = GrantedRole(module, roleDefinition)
    val role = gr.build()
    role.getName should be("AssignmentSubmitterRoleDefinition")
    role.explicitPermissionsAsList.contains((Permissions.Submission.Create, Some(module))) should be(true)
  }

  @Test def buildWithOverride: Unit = {
    val gr = GrantedRole(module, roleDefinition)

    val custom = new CustomRoleDefinition
    custom.department = dept
    custom.baseRoleDefinition = roleDefinition
    custom.replacesBaseDefinition = true
    custom.name = "Custom definition"

    val noSubmit = new RoleOverride
    noSubmit.overrideType = false
    noSubmit.permission = Permissions.Submission.Create
    noSubmit.customRoleDefinition = custom

    custom.overrides.add(noSubmit)

    dept.customRoleDefinitions.add(custom)

    val role = gr.build()
    role.getName should be("Custom definition")
    role.explicitPermissionsAsList.contains((Permissions.Submission.Create, Some(module))) should be(false)
  }

}