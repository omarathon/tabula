package uk.ac.warwick.tabula.attendance.web.controllers

import uk.ac.warwick.tabula.{Mockito, CurrentUser, TestBase}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.helpers.Tap
import Tap.tap
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.JavaImports._

class HomeControllerTest extends  TestBase with Mockito{

	class Fixture{
		val command = mock[Appliable[Map[String, Set[Department]]]]

		val student = new User().tap(_.setStudent(true))
		val pgr = new User().tap(u=>{
			u.setStaff(true)
			u.setExtraProperties(JMap("warwickitsclass"->"PG(R)"))
		})
		val staff = new User().tap(_.setStaff(true))
		val external = new User()
	}


	@Test
	def studentsGetRedirectedToProfileView(){new Fixture{
		withCurrentUser(new CurrentUser(student,student)){

			val mav = new HomeController().home(command)
			mav.viewName should be("redirect:/profile")

			there were no(command).apply()
		}
	}}

	@Test
	def PGRsGetAdminViewWithHasOwnPointsFlag(){new Fixture{
		withCurrentUser(new CurrentUser(pgr,pgr)){

			command.apply() returns Map(
				"View"->Set(new Department().tap(_.code="xx")),
				"Manage"->Set(new Department().tap(_.code="xx"))
			)
			val mav = new HomeController().home(command)
			mav.viewName should be("home/home")
			mav.toModel("hasOwnMonitoringPoints") should be(JBoolean(Some(true)))
		}
	}}

	@Test
  def staffWithSingleDepartmentViewAndNoDepartmentManageRightsGetsRedirectToDepartment(){new Fixture{
		withCurrentUser(new CurrentUser(staff,staff)){

			command.apply() returns Map(
			 	"View"->Set(new Department().tap(_.code="xx")),
			  "Manage"->Set.empty
			)
			val mav = new HomeController().home(command)

			mav.viewName should be("redirect:/xx")

			there was one(command).apply()
		}
	}}

	@Test
  def staffWithMultiplePermissionsGetHomeView(){new Fixture{
		withCurrentUser(new CurrentUser(staff,staff)){

			val permisisons = Map(
				"View"->Set(new Department().tap(_.code="xx")),
				"Manage"->Set(new Department().tap(_.code="xx"))
			)
			command.apply() returns permisisons
			val mav = new HomeController().home(command)

			mav.viewName should be("home/home")
			mav.toModel("permissionMap")  should be(permisisons)

			there was one(command).apply()
		}
	}}

	@Test
	def nonStaffOrStudentGetsPermissionDenied(){new Fixture{
		withCurrentUser(new CurrentUser(external,external)){

			val mav = new HomeController().home(command)
			mav.viewName should be("home/nopermission")

			there were no(command).apply()
		}
	}}

}
