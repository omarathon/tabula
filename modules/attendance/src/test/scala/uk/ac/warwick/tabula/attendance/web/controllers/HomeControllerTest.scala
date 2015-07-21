package uk.ac.warwick.tabula.attendance.web.controllers

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.helpers.Tap
import Tap.tap
import uk.ac.warwick.tabula.data.model.{StudentRelationshipType, Department}
import uk.ac.warwick.tabula.attendance.commands.{HomeCommandState, HomeCommand}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.services._
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
			with CourseAndRouteServiceComponent with ProfileServiceComponent with RelationshipServiceComponent {

			var relationshipService: RelationshipService = _
			var profileService: ProfileService = _
			var moduleAndDepartmentService: ModuleAndDepartmentService = _
			var courseAndRouteService: CourseAndRouteService = _

			def apply() = info
		}
		val departmentCode = "xx"
		val relationshipType = mock[StudentRelationshipType]

		val controller = new HomeController
		controller.features = emptyFeatures
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

		val mav = controller.home(command, null)
		mav.viewName should be("redirect:/attendance/profile")

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

		val mav = controller.home(command, null)
		mav.viewName should be(s"redirect:/attendance/view/$departmentCode")

	}}

	@Test
	def onlyOneViewPermissionRedirectedToViewDepartmentOldAM(){new Fixture{
		info = HomeInformation(
			hasProfile = false,
			viewPermissions = Set(new Department().tap(_.code=departmentCode)),
			managePermissions = Set(),
			allRelationshipTypes = Seq(),
			relationshipTypesMap = Map()
		)

		controller.features.attendanceMonitoringVersion2 = false
		val mav = controller.home(command, null)
		mav.viewName should be(s"redirect:/attendance/$departmentCode")

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

		val mav = controller.home(command, null)
		mav.viewName should be(s"redirect:/attendance/manage/$departmentCode")

	}}

	@Test
	def onlyOneManagePermissionRedirectedToManageDepartmentOldAM(){new Fixture{
		info = HomeInformation(
			hasProfile = false,
			viewPermissions = Set(),
			managePermissions = Set(new Department().tap(_.code=departmentCode)),
			allRelationshipTypes = Seq(),
			relationshipTypesMap = Map()
		)

		controller.features.attendanceMonitoringVersion2 = false
		val mav = controller.home(command, null)
		mav.viewName should be(s"redirect:/attendance/manage/$departmentCode/2013")

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

		val mav = controller.home(command, null)
		mav.viewName should be("home")

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

		val mav = controller.home(command, null)
		mav.viewName should be("home")

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

		val mav = controller.home(command, null)
		mav.viewName should be("home")

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

		val mav = controller.home(command, null)
		mav.viewName should be("home")

	}}


}
