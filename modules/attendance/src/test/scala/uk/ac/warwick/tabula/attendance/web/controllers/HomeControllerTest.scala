package uk.ac.warwick.tabula.attendance.web.controllers

import uk.ac.warwick.tabula.{Mockito, CurrentUser, TestBase}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.helpers.Tap
import Tap.tap
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.JavaImports._

class HomeControllerTest extends TestBase with Mockito{

	class Fixture{
		val command = mock[Appliable[(Boolean, Map[String, Set[Department]])]]
		val departmentCode = "xx"
	}


	@Test
	def onlyProfileRedirectedToProfileView(){new Fixture{
		command.apply() returns Pair(
			true,
			Map(
				"ViewPermissions" -> Set(),
				"ManagePermissions" -> Set()
			)
		)

		val mav = new HomeController().home(command)
		mav.viewName should be("redirect:/profile")

	}}

	@Test
	def onlyOneViewPermissionRedirectedToViewDepartment(){new Fixture{

		command.apply() returns Pair(
			false,
			Map(
				"ViewPermissions" -> Set(new Department().tap(_.code=departmentCode)),
				"ManagePermissions" -> Set()
			)
		)

		val mav = new HomeController().home(command)
		mav.viewName should be(s"redirect:/$departmentCode")

	}}

	@Test
	def onlyOneManagePermissionRedirectedToManageDepartment(){new Fixture{
		command.apply() returns Pair(
			false,
			Map(
				"ViewPermissions" -> Set(),
				"ManagePermissions" -> Set(new Department().tap(_.code=departmentCode))
			)
		)

		val mav = new HomeController().home(command)
		mav.viewName should be(s"redirect:/manage/$departmentCode")

	}}

	@Test
	def noPermissions(){new Fixture{
		command.apply() returns Pair(
			false,
			Map(
				"ViewPermissions" -> Set(),
				"ManagePermissions" -> Set()
			)
		)

		val mav = new HomeController().home(command)
		mav.viewName should be("home/home")

	}}

	@Test
	def viewAndManageAndProfileShoHome(){new Fixture{
		command.apply() returns Pair(
			true,
			Map(
				"ViewPermissions" -> Set(new Department().tap(_.code=departmentCode)),
				"ManagePermissions" -> Set(new Department().tap(_.code=departmentCode))
			)
		)

		val mav = new HomeController().home(command)
		mav.viewName should be("home/home")

	}}


}
