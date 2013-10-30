package uk.ac.warwick.tabula.attendance.web.controllers

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.helpers.Tap
import Tap.tap
import uk.ac.warwick.tabula.data.model.{StudentRelationshipType, Department}
import uk.ac.warwick.tabula.attendance.commands.{HomeCommandState, HomeCommand}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService, ModuleAndDepartmentService, RelationshipServiceComponent, ModuleAndDepartmentServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.attendance.commands.HomeInformation
import uk.ac.warwick.tabula.attendance.commands.HomeInformation

class HomeControllerTest extends TestBase with Mockito{

	class Fixture{
		var info = HomeInformation(
			hasProfile = false,
			viewPermissions = Set(),
			managePermissions = Set(),
			allRelationshipTypes = Seq(),
			relationshipTypesMap = Map()
		)
		
		val command = new HomeCommand(null) with Appliable[HomeInformation] with HomeCommandState with ModuleAndDepartmentServiceComponent
			with ProfileServiceComponent with RelationshipServiceComponent {

			var relationshipService: RelationshipService = _
			var profileService: ProfileService = _
			var moduleAndDepartmentService: ModuleAndDepartmentService = _

			def apply() = info
		}
		val departmentCode = "xx"
		val relationshipType = mock[StudentRelationshipType]
	}


	@Test
	def onlyProfileRedirectedToProfileView(){new Fixture{
		info = HomeInformation(
			hasProfile = true,
			viewPermissions = Set(),
			managePermissions = Set(),
			allRelationshipTypes = Seq(),
			relationshipTypesMap = Map()
		)

		val mav = new HomeController().home(command)
		mav.viewName should be("redirect:/profile")

	}}

	@Test
	def onlyOneViewPermissionRedirectedToViewDepartment(){new Fixture{
		info = HomeInformation(
			hasProfile = false,
			viewPermissions = Set(new Department().tap(_.code=departmentCode)),
			managePermissions = Set(),
			allRelationshipTypes = Seq(),
			relationshipTypesMap = Map()
		)

		val mav = new HomeController().home(command)
		mav.viewName should be(s"redirect:/$departmentCode")

	}}

	@Test
	def onlyOneManagePermissionRedirectedToManageDepartment(){new Fixture{
		info = HomeInformation(
			hasProfile = false,
			viewPermissions = Set(),
			managePermissions = Set(new Department().tap(_.code=departmentCode)),
			allRelationshipTypes = Seq(),
			relationshipTypesMap = Map()
		)

		val mav = new HomeController().home(command)
		mav.viewName should be(s"redirect:/manage/$departmentCode")

	}}

	@Test
	def noPermissions(){new Fixture{
		info = HomeInformation(
			hasProfile = false,
			viewPermissions = Set(),
			managePermissions = Set(),
			allRelationshipTypes = Seq(),
			relationshipTypesMap = Map()
		)

		val mav = new HomeController().home(command)
		mav.viewName should be("home/home")

	}}

	@Test
	def viewPermissionsAndProfileShowHome(){new Fixture{
		info = HomeInformation(
			hasProfile = true,
			viewPermissions = Set(new Department().tap(_.code=departmentCode)),
			managePermissions = Set(),
			allRelationshipTypes = Seq(),
			relationshipTypesMap = Map()
		)

		val mav = new HomeController().home(command)
		mav.viewName should be("home/home")

	}}

	@Test
	def managePermissionsAndProfileShowHome(){new Fixture{
		info = HomeInformation(
			hasProfile = true,
			viewPermissions = Set(),
			managePermissions = Set(new Department().tap(_.code=departmentCode)),
			allRelationshipTypes = Seq(),
			relationshipTypesMap = Map()
		)

		val mav = new HomeController().home(command)
		mav.viewName should be("home/home")

	}}

	@Test
	def relationshipsAndProfileShowHome(){new Fixture{
		info = HomeInformation(
			hasProfile = true,
			viewPermissions = Set(),
			managePermissions = Set(),
			allRelationshipTypes = Seq(),
			relationshipTypesMap = Map(relationshipType -> true)
		)

		val mav = new HomeController().home(command)
		mav.viewName should be("home/home")

	}}


}
