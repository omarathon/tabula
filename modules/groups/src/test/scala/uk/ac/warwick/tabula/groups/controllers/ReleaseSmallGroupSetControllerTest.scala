package uk.ac.warwick.tabula.groups.controllers

import junit.framework.Test
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.groups.web.controllers.admin.ReleaseSmallGroupSetController
import uk.ac.warwick.tabula.groups.web.controllers.admin.ReleaseAllSmallGroupSetsController
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import org.mockito.Mockito.verify
import org.mockito.Mockito.times
import org.mockito.Mockito.when
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.{Department, Module}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.groups.commands.admin.ReleaseSmallGroupSetCommand
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.{ViewModule, ViewSet}
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel
import scala.collection.mutable

class ReleaseSmallGroupSetControllerTest extends TestBase with Mockito {

  @Test
  def createsCommandObject() {
    withUser("test") {
      val set = new SmallGroupSet
      val controller = new ReleaseSmallGroupSetController
      controller.getReleaseGroupSetCommand(set) should not be (null)
    }
  }

  @Test
  def showsForm() {
    withUser("test") {
      val controller = new ReleaseSmallGroupSetController
      val cmd = mock[ReleaseSmallGroupSetCommand]
      controller.form(cmd).viewName should be("admin/groups/release")
      controller.form(cmd).map should be(Map())
    }
  }

  @Test
  def invokesCommand() {
    withUser("test") {
      val controller = new ReleaseSmallGroupSetController
			val cmd = mock[ReleaseSmallGroupSetCommand]
      when(cmd.apply()).thenReturn(Seq(new SmallGroupSet()))

      controller.submit(cmd).viewName should be("admin/groups/single_groupset")

      verify(cmd, times(1)).apply()
    }
  }

	@Test
	def returnsContextAsExpected() {
		withUser("test") {
			val controller = new ReleaseSmallGroupSetController
			val cmd = mock[ReleaseSmallGroupSetCommand]
			val set = new SmallGroupSet()
			set.module = new Module()
			cmd.apply() returns (Seq(set))
			cmd.describeOutcome returns Some("hello")
			val context = controller.submit(cmd).map

			val expectedViewSet = new ViewSet(set,set.groups.asScala,GroupsViewModel.Tutor)
			context("notificationSentMessage") should be(Some("hello"))
			context("groupsetItem") should be(expectedViewSet)
			context("moduleItem") should be(new ViewModule(set.module,Seq(expectedViewSet),true))
		}
	}


	@Test
  def moduleListViewModelConvertsModulesToGroupSets() {
    val mod1 = new Module()
    mod1.groupSets = Seq(new SmallGroupSet, new SmallGroupSet).asJava
    val mod2 = new Module()
    mod2.groupSets = Seq(new SmallGroupSet, new SmallGroupSet).asJava

    val viewModel = new ReleaseAllSmallGroupSetsController().newViewModel
    viewModel.checkedModules = JArrayList(mod1, mod2)

    viewModel.smallGroupSets().size should be(4)
  }

  @Test
  def batchControllerCreatesViewModel() {
    new ReleaseAllSmallGroupSetsController().newViewModel() should not be null
  }

  @Test
  def batchControllerShowsFormWithDepartment() {
    val controller = new ReleaseAllSmallGroupSetsController()
    val model = controller.newViewModel()
    val department = new Department
    department.code = "xyz"
    val mav = controller.form(model, department)
    mav.toModel.get("department") should be(Some(department))
    mav.viewName should be("admin/groups/bulk-release")
  }


  @Test
  def batchControllerInvokesCommand() {
    withUser("test") {

      val controller = new ReleaseAllSmallGroupSetsController()
      val command = mock[Appliable[Seq[SmallGroupSet]]]
      val model = mock[controller.ModuleListViewModel]
      when(model.createCommand(any[User])).thenReturn(command)
      controller.submit(model, new Department)
      verify(command, times(1)).apply()
    }
  }

  @Test
  def batchControllerReturnsNewFormWithFlashMessage(){
    withUser("test") {

      val controller = new ReleaseAllSmallGroupSetsController()
      val command = mock[Appliable[Seq[SmallGroupSet]]]
      val model = mock[controller.ModuleListViewModel]
      val department = new Department
      department.code = "xyz"
      when(model.createCommand(any[User])).thenReturn(command)

      val formView = controller.submit(model, department)

      formView.viewName should be("redirect:/admin/department/xyz/groups/release")
      formView.map.get("batchReleaseSuccess") should be(Some(true))
    }
  }
}
