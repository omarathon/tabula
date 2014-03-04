package uk.ac.warwick.tabula.admin.web.controllers.department

import uk.ac.warwick.tabula.{MockUserLookup, Fixtures, TestBase, Mockito}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.commands.permissions.{RevokeRoleCommandState, GrantRoleCommandState}
import uk.ac.warwick.userlookup.User
import scala.collection.mutable
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import org.springframework.validation.BindException

class DepartmentPermissionControllerTest extends TestBase with Mockito {

	val listController = new DepartmentPermissionController
	val addController = new DepartmentAddPermissionController
	val removeController = new DepartmentRemovePermissionController

	trait Fixture {
		val department = Fixtures.department("in")

		val userLookup = new MockUserLookup
		Seq(listController, addController, removeController).foreach { controller => controller.userLookup = userLookup }
	}

	@Test def createCommands {
		Seq(listController, addController, removeController).foreach { controller => new Fixture {
			val addCommand = controller.addCommandModel(department)
			val removeCommand = controller.removeCommandModel(department)

			addCommand should be (anInstanceOf[Appliable[GrantedRole[Department]]])
			addCommand should be (anInstanceOf[GrantRoleCommandState[Department]])

			removeCommand should be (anInstanceOf[Appliable[GrantedRole[Department]]])
			removeCommand should be (anInstanceOf[RevokeRoleCommandState[Department]])
		}}
	}

	@Test def list { new Fixture {
		val mav = listController.permissionsForm(department, Array(), null, null)
		mav.viewName should be ("admin/department/permissions")
		mav.toModel("department") should be (department)
		mav.toModel("users").asInstanceOf[mutable.Map[String, User]] should be ('empty)
		mav.toModel("role") should be (Some(null))
		mav.toModel("action") should be (null.asInstanceOf[String])
	}}

	@Test def listFromRedirect { new Fixture {
		userLookup.registerUsers("cuscav", "cusebr")

		val mav = listController.permissionsForm(department, Array("cuscav", "cusebr"), DepartmentalAdministratorRoleDefinition, "add")
		mav.viewName should be ("admin/department/permissions")
		mav.toModel("department") should be (department)
		mav.toModel("users") should be (mutable.Map(
			"cuscav" -> userLookup.getUserByUserId("cuscav"),
			"cusebr" -> userLookup.getUserByUserId("cusebr")
		))
		mav.toModel("role") should be (Some(DepartmentalAdministratorRoleDefinition))
		mav.toModel("action") should be ("add")
	}}

	@Test def add { new Fixture {
		val command = mock[Appliable[GrantedRole[Department]] with GrantRoleCommandState[Department]]
		command.scope returns (department)

		val errors = new BindException(command, "command")

		val mav = addController.addPermission(command, errors)
	}}

}
