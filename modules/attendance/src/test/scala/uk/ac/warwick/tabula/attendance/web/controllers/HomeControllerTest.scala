package uk.ac.warwick.tabula.attendance.web.controllers

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.helpers.Tap
import Tap.tap
import uk.ac.warwick.tabula.data.model.{StudentRelationshipType, Department}
import uk.ac.warwick.tabula.attendance.commands.{HomeCommandState, HomeCommand}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService, ModuleAndDepartmentService, RelationshipServiceComponent, ModuleAndDepartmentServiceComponent, ProfileServiceComponent}

class HomeControllerTest extends TestBase with Mockito{

	class Fixture{
		val command = new HomeCommand(null) with Appliable[Unit] with HomeCommandState with ModuleAndDepartmentServiceComponent
			with ProfileServiceComponent with RelationshipServiceComponent {

			var relationshipService: RelationshipService = _
			var profileService: ProfileService = _
			var moduleAndDepartmentService: ModuleAndDepartmentService = _

			def apply() = {}
		}
		val departmentCode = "xx"
		val relationshipType = mock[StudentRelationshipType]
	}


	@Test
	def onlyProfileRedirectedToProfileView(){new Fixture{
		command.hasProfile = true
		command.viewPermissions = Set()
		command.managePermissions = Set()
		command.relationshipTypesMap = Map()

		val mav = new HomeController().home(command)
		mav.viewName should be("redirect:/profile")

	}}

	@Test
	def onlyOneViewPermissionRedirectedToViewDepartment(){new Fixture{
		command.hasProfile = false
		command.viewPermissions = Set(new Department().tap(_.code=departmentCode))
		command.managePermissions = Set()
		command.relationshipTypesMap = Map()

		val mav = new HomeController().home(command)
		mav.viewName should be(s"redirect:/$departmentCode")

	}}

	@Test
	def onlyOneManagePermissionRedirectedToManageDepartment(){new Fixture{
		command.hasProfile = false
		command.viewPermissions = Set()
		command.managePermissions = Set(new Department().tap(_.code=departmentCode))
		command.relationshipTypesMap = Map()

		val mav = new HomeController().home(command)
		mav.viewName should be(s"redirect:/manage/$departmentCode")

	}}

	@Test
	def noPermissions(){new Fixture{
		command.hasProfile = false
		command.viewPermissions = Set()
		command.managePermissions = Set()
		command.relationshipTypesMap = Map()

		val mav = new HomeController().home(command)
		mav.viewName should be("home/home")

	}}

	@Test
	def viewPermissionsAndProfileShowHome(){new Fixture{
		command.hasProfile = true
		command.viewPermissions = Set(new Department().tap(_.code=departmentCode))
		command.managePermissions = Set()
		command.relationshipTypesMap = Map()

		val mav = new HomeController().home(command)
		mav.viewName should be("home/home")

	}}

	@Test
	def managePermissionsAndProfileShowHome(){new Fixture{
		command.hasProfile = true
		command.viewPermissions = Set()
		command.managePermissions = Set(new Department().tap(_.code=departmentCode))
		command.relationshipTypesMap = Map()

		val mav = new HomeController().home(command)
		mav.viewName should be("home/home")

	}}

	@Test
	def relationshipsAndProfileShowHome(){new Fixture{
		command.hasProfile = true
		command.viewPermissions = Set()
		command.managePermissions = Set()
		command.relationshipTypesMap = Map(relationshipType -> true)

		val mav = new HomeController().home(command)
		mav.viewName should be("home/home")

	}}


}
