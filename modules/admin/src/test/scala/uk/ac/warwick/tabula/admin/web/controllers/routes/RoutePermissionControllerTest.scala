package uk.ac.warwick.tabula.admin.web.controllers.routes

import uk.ac.warwick.tabula.{MockUserLookup, Fixtures, TestBase, Mockito}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.data.model.permissions.{RouteGrantedRole, GrantedRole}
import uk.ac.warwick.tabula.commands.permissions.{RevokeRoleCommandState, GrantRoleCommandState}
import uk.ac.warwick.userlookup.User
import scala.collection.mutable
import uk.ac.warwick.tabula.roles.RouteManagerRoleDefinition
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.services.permissions.PermissionsServiceComponent

class RoutePermissionControllerTest extends TestBase with Mockito {

	val listController = new RoutePermissionController
	val addController = new RouteAddPermissionController
	val removeController = new RouteRemovePermissionController

	trait Fixture {
		val department = Fixtures.department("in")
		val route = Fixtures.route("i100")
		route.department = department

		val userLookup = new MockUserLookup
		Seq(listController, addController, removeController).foreach { controller => controller.userLookup = userLookup }

		userLookup.registerUsers("cuscav", "cusebr")
	}

	@Test def createCommands {
		Seq(listController, addController, removeController).foreach { controller => new Fixture {
			val addCommand = controller.addCommandModel(route)
			val removeCommand = controller.removeCommandModel(route)

			addCommand should be (anInstanceOf[Appliable[GrantedRole[Route]]])
			addCommand should be (anInstanceOf[GrantRoleCommandState[Route]])

			removeCommand should be (anInstanceOf[Appliable[GrantedRole[Route]]])
			removeCommand should be (anInstanceOf[RevokeRoleCommandState[Route]])
		}}
	}

	@Test def list { new Fixture {
		val mav = listController.permissionsForm(route, Array(), null, null)
		mav.viewName should be ("admin/routes/permissions")
		mav.toModel("route") should be (route)
		mav.toModel("users").asInstanceOf[mutable.Map[String, User]] should be ('empty)
		mav.toModel("role") should be (Some(null))
		mav.toModel("action") should be (null.asInstanceOf[String])
	}}

	@Test def listFromRedirect { new Fixture {
		val mav = listController.permissionsForm(route, Array("cuscav", "cusebr"), RouteManagerRoleDefinition, "add")
		mav.viewName should be ("admin/routes/permissions")
		mav.toModel("route") should be (route)
		mav.toModel("users") should be (mutable.Map(
			"cuscav" -> userLookup.getUserByUserId("cuscav"),
			"cusebr" -> userLookup.getUserByUserId("cusebr")
		))
		mav.toModel("role") should be (Some(RouteManagerRoleDefinition))
		mav.toModel("action") should be ("add")
	}}

	@Test def add { new Fixture {
		val addedRole = new RouteGrantedRole(route, RouteManagerRoleDefinition)

		val command = new Appliable[GrantedRole[Route]] with GrantRoleCommandState[Route] with PermissionsServiceComponent {
			val permissionsService = null
			def scope = route
			def grantedRole = Some(addedRole)
			def apply = addedRole
		}
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")

		val errors = new BindException(command, "command")

		val mav = addController.addPermission(command, errors)
		mav.viewName should be ("admin/routes/permissions")
		mav.toModel("route") should be (route)
		mav.toModel("users") should be (mutable.Map(
			"cuscav" -> userLookup.getUserByUserId("cuscav"),
			"cusebr" -> userLookup.getUserByUserId("cusebr")
		))
		mav.toModel("role") should be (Some(RouteManagerRoleDefinition))
		mav.toModel("action") should be ("add")
	}}

	@Test def addValidationErrors { new Fixture {
		val addedRole = new RouteGrantedRole(route, RouteManagerRoleDefinition)

		val command = new Appliable[GrantedRole[Route]] with GrantRoleCommandState[Route] with PermissionsServiceComponent {
			val permissionsService = null
			def scope = route
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
		mav.viewName should be ("admin/routes/permissions")
		mav.toModel("route") should be (route)
		mav.toModel.contains("users") should be (false)
		mav.toModel.contains("role") should be (false)
		mav.toModel.contains("action") should be (false)
	}}

	@Test def remove { new Fixture {
		val removedRole = new RouteGrantedRole(route, RouteManagerRoleDefinition)

		val command = new Appliable[GrantedRole[Route]] with RevokeRoleCommandState[Route] with PermissionsServiceComponent {
			val permissionsService = null
			def scope = route
			def grantedRole = Some(removedRole)
			def apply = removedRole
		}
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")

		val errors = new BindException(command, "command")

		val mav = removeController.removePermission(command, errors)
		mav.viewName should be ("admin/routes/permissions")
		mav.toModel("route") should be (route)
		mav.toModel("users") should be (mutable.Map(
			"cuscav" -> userLookup.getUserByUserId("cuscav"),
			"cusebr" -> userLookup.getUserByUserId("cusebr")
		))
		mav.toModel("role") should be (Some(RouteManagerRoleDefinition))
		mav.toModel("action") should be ("remove")
	}}

	@Test def removeValidationErrors { new Fixture {
		val removedRole = new RouteGrantedRole(route, RouteManagerRoleDefinition)

		val command = new Appliable[GrantedRole[Route]] with RevokeRoleCommandState[Route] with PermissionsServiceComponent {
			val permissionsService = null
			def scope = route
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
		mav.viewName should be ("admin/routes/permissions")
		mav.toModel("route") should be (route)
		mav.toModel.contains("users") should be (false)
		mav.toModel.contains("role") should be (false)
		mav.toModel.contains("action") should be (false)
	}}

}
