package uk.ac.warwick.tabula.groups.controllers

import uk.ac.warwick.tabula.{CurrentUser, TestBase, Mockito}
import uk.ac.warwick.tabula.groups.web.controllers.admin.OpenAllSmallGroupSetsController
import uk.ac.warwick.tabula.groups.SmallGroupFixture
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupSet, SmallGroupAllocationMethod}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.groups.commands.admin.OpenSmallGroupSet
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSetSelfSignUpState
import uk.ac.warwick.tabula.data.model.Department

class OpenSmallGroupSetsControllerTest extends TestBase with Mockito {

	@Test
	def createsViewModelAbleToBuildCommand() {
		val controller = new OpenAllSmallGroupSetsController()
		controller.newViewModelOpen(new Department, SmallGroupSetSelfSignUpState.Open) should be(anInstanceOf[controller.GroupsetListViewModel])
		controller.newViewModelOpen(new Department, SmallGroupSetSelfSignUpState.Open).createCommand(new User, Nil) should be(anInstanceOf[OpenSmallGroupSet])
	}


	@Test
	def exposesOnlySelfSignupGroupSetsToForm() {
		new SmallGroupFixture {

			groupSet1.allocationMethod = SmallGroupAllocationMethod.StudentSignUp
			groupSet2.allocationMethod = SmallGroupAllocationMethod.Manual
			department.modules = Seq(groupSet1.module, groupSet2.module).asJava
			department.code = "XYZ"

			val controller = new OpenAllSmallGroupSetsController()
			val mav = controller.form(controller.newViewModelOpen(department, SmallGroupSetSelfSignUpState.Open), department)

			mav.map("groupSets") should be(Seq(groupSet1))
		}
	}

	@Test
	def exposesDepartmentToForm() {
		new SmallGroupFixture {
			val controller = new OpenAllSmallGroupSetsController()
			department.code = "XYZ"
			
			val mav = controller.form(controller.newViewModelOpen(department, SmallGroupSetSelfSignUpState.Open), department)

			mav.map("department") should be(department)

		}
	}

	@Test
	def exposesFlashStatusToForm() {
		new SmallGroupFixture {

			val controller = new OpenAllSmallGroupSetsController()
			department.code = "XYZ"

			controller.form(controller.newViewModelOpen(department, SmallGroupSetSelfSignUpState.Open), department).map("showFlash") should be(JBoolean(Some(false)))
			controller.form(controller.newViewModelOpen(department, SmallGroupSetSelfSignUpState.Open), department, true).map("showFlash") should be(JBoolean(Some(true)))
		}
	}

	@Test
	def usesCorrectViewNameForForm() {
		new SmallGroupFixture {

			val controller = new OpenAllSmallGroupSetsController()
			department.code = "XYZ"
			
			controller.form(controller.newViewModelOpen(department, SmallGroupSetSelfSignUpState.Open), department).viewName should be("admin/groups/bulk-open")
		}
	}

	@Test
	def viewModelPassesApplyOntoCommand() {
		val controller = new OpenAllSmallGroupSetsController()
		val mockCommand = mock[Appliable[Seq[SmallGroupSet]]]

		val model = new controller.GroupsetListViewModel((u, s) => mockCommand, SmallGroupSetSelfSignUpState.Open)

		model.applyCommand(new User)

		there was one(mockCommand).apply()
	}

	@Test
	def submitCallsApplyOnViewModel() {
		new SmallGroupFixture {
			department.code = "XYZ"
			val apparentUser = new User("apparent")
			val user = new CurrentUser(new User, apparentUser)
			withCurrentUser(user) {
				val controller = new OpenAllSmallGroupSetsController()
				val viewModel = mock[controller.GroupsetListViewModel]
				viewModel.getName returns ("open")
								
				controller.submit(viewModel, department)

				there was one(viewModel).applyCommand(apparentUser)
			}
		}
	}

	@Test
	def submitSendsRedirectBackToOpenGroupsPage() {
		new SmallGroupFixture {
			department.code = "XYZ"
			val user = new CurrentUser(new User, new User)
			withCurrentUser(user) {
				val controller = new OpenAllSmallGroupSetsController()
				val mockCommand = mock[Appliable[Seq[SmallGroupSet]]]
				val viewModel = new controller.GroupsetListViewModel((u, s) => mockCommand, SmallGroupSetSelfSignUpState.Open)

				val mav = controller.submit(viewModel, department)

				mav.viewName should be("redirect:/admin/department/XYZ/groups/selfsignup/open")
				mav.map("batchOpenSuccess") should be (JBoolean(Some(true)))
			}
		}
	}
}
