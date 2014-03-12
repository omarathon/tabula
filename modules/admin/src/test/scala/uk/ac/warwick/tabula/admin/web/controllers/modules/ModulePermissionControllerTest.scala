package uk.ac.warwick.tabula.admin.web.controllers.modules

import uk.ac.warwick.tabula.{MockUserLookup, Fixtures, TestBase, Mockito}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.permissions.{ModuleGrantedRole, GrantedRole}
import uk.ac.warwick.tabula.commands.permissions.{RevokeRoleCommandState, GrantRoleCommandState}
import uk.ac.warwick.userlookup.User
import scala.collection.mutable
import uk.ac.warwick.tabula.roles.ModuleManagerRoleDefinition
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.services.permissions.PermissionsServiceComponent

class ModulePermissionControllerTest extends TestBase with Mockito {

	val listController = new ModulePermissionController
	val addController = new ModuleAddPermissionController
	val removeController = new ModuleRemovePermissionController

	trait Fixture {
		val department = Fixtures.department("in")
		val module = Fixtures.module("in101")
		module.department = department

		val userLookup = new MockUserLookup
		Seq(listController, addController, removeController).foreach { controller => controller.userLookup = userLookup }

		userLookup.registerUsers("cuscav", "cusebr")
	}

	@Test def createCommands {
		Seq(listController, addController, removeController).foreach { controller => new Fixture {
			val addCommand = controller.addCommandModel(module)
			val removeCommand = controller.removeCommandModel(module)

			addCommand should be (anInstanceOf[Appliable[GrantedRole[Module]]])
			addCommand should be (anInstanceOf[GrantRoleCommandState[Module]])

			removeCommand should be (anInstanceOf[Appliable[GrantedRole[Module]]])
			removeCommand should be (anInstanceOf[RevokeRoleCommandState[Module]])
		}}
	}

	@Test def list { new Fixture {
		val mav = listController.permissionsForm(module, Array(), null, null)
		mav.viewName should be ("admin/modules/permissions")
		mav.toModel("module") should be (module)
		mav.toModel("users").asInstanceOf[mutable.Map[String, User]] should be ('empty)
		mav.toModel("role") should be (Some(null))
		mav.toModel("action") should be (null.asInstanceOf[String])
	}}

	@Test def listFromRedirect { new Fixture {
		val mav = listController.permissionsForm(module, Array("cuscav", "cusebr"), ModuleManagerRoleDefinition, "add")
		mav.viewName should be ("admin/modules/permissions")
		mav.toModel("module") should be (module)
		mav.toModel("users") should be (mutable.Map(
			"cuscav" -> userLookup.getUserByUserId("cuscav"),
			"cusebr" -> userLookup.getUserByUserId("cusebr")
		))
		mav.toModel("role") should be (Some(ModuleManagerRoleDefinition))
		mav.toModel("action") should be ("add")
	}}

	@Test def add { new Fixture {
		val addedRole = new ModuleGrantedRole(module, ModuleManagerRoleDefinition)

		val command = new Appliable[GrantedRole[Module]] with GrantRoleCommandState[Module] with PermissionsServiceComponent {
			val permissionsService = null
			def scope = module
			def grantedRole = Some(addedRole)
			def apply = addedRole
		}
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")

		val errors = new BindException(command, "command")

		val mav = addController.addPermission(command, errors)
		mav.viewName should be ("admin/modules/permissions")
		mav.toModel("module") should be (module)
		mav.toModel("users") should be (mutable.Map(
			"cuscav" -> userLookup.getUserByUserId("cuscav"),
			"cusebr" -> userLookup.getUserByUserId("cusebr")
		))
		mav.toModel("role") should be (Some(ModuleManagerRoleDefinition))
		mav.toModel("action") should be ("add")
	}}

	@Test def addValidationErrors { new Fixture {
		val addedRole = new ModuleGrantedRole(module, ModuleManagerRoleDefinition)

		val command = new Appliable[GrantedRole[Module]] with GrantRoleCommandState[Module] with PermissionsServiceComponent {
			val permissionsService = null
			def scope = module
			def grantedRole = Some(addedRole)
			def apply() = {
				fail("Should not be called")
				null
			}
		}
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")

		val errors = new BindException(command, "command")

		errors.reject("fail")

		val mav = addController.addPermission(command, errors)
		mav.viewName should be ("admin/modules/permissions")
		mav.toModel("module") should be (module)
		mav.toModel.contains("users") should be (false)
		mav.toModel.contains("role") should be (false)
		mav.toModel.contains("action") should be (false)
	}}

	@Test def remove { new Fixture {
		val removedRole = new ModuleGrantedRole(module, ModuleManagerRoleDefinition)

		val command = new Appliable[GrantedRole[Module]] with RevokeRoleCommandState[Module] with PermissionsServiceComponent {
			val permissionsService = null
			def scope = module
			def grantedRole = Some(removedRole)
			def apply = removedRole
		}
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")

		val errors = new BindException(command, "command")

		val mav = removeController.removePermission(command, errors)
		mav.viewName should be ("admin/modules/permissions")
		mav.toModel("module") should be (module)
		mav.toModel("users") should be (mutable.Map(
			"cuscav" -> userLookup.getUserByUserId("cuscav"),
			"cusebr" -> userLookup.getUserByUserId("cusebr")
		))
		mav.toModel("role") should be (Some(ModuleManagerRoleDefinition))
		mav.toModel("action") should be ("remove")
	}}

	@Test def removeValidationErrors { new Fixture {
		val removedRole = new ModuleGrantedRole(module, ModuleManagerRoleDefinition)

		val command = new Appliable[GrantedRole[Module]] with RevokeRoleCommandState[Module] with PermissionsServiceComponent {
			val permissionsService = null
			def scope = module
			def grantedRole = Some(removedRole)
			def apply() = {
				fail("Should not be called")
				null
			}
		}
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")

		val errors = new BindException(command, "command")
		errors.reject("fail")

		val mav = removeController.removePermission(command, errors)
		mav.viewName should be ("admin/modules/permissions")
		mav.toModel("module") should be (module)
		mav.toModel.contains("users") should be (false)
		mav.toModel.contains("role") should be (false)
		mav.toModel.contains("action") should be (false)
	}}

}
