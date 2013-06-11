package uk.ac.warwick.tabula.groups.controllers

import junit.framework.Test
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.groups.web.controllers.admin.ReleaseSmallGroupSetController
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import org.mockito.Mockito.verify
import org.mockito.Mockito.times
import org.mockito.Mockito.when
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.commands.Appliable

class ReleaseSmallGroupSetControllerTest extends TestBase with Mockito{

  @Test
  def createsCommandObject() {
    withUser("test") {
      val set = new SmallGroupSet
      val controller = new ReleaseSmallGroupSetController
      controller.getReleaseGroupSetCommand(set) should not be (null)
    }
  }

  @Test
  def showsForm(){
    withUser("test"){
      val controller = new ReleaseSmallGroupSetController
      val cmd = mock[Appliable[SmallGroupSet]]
      controller.form(cmd).viewName should be("admin/groups/release")
      controller.form(cmd).map should be(Map())
    }
  }

  @Test
  def invokesCommand(){
    withUser("test"){
      val controller = new ReleaseSmallGroupSetController
      val cmd= mock[Appliable[SmallGroupSet]]
      when(cmd.apply()).thenReturn(new SmallGroupSet())

      controller.submit(cmd).viewName should be("ajax_success")

      verify(cmd, times(1)).apply()
      controller.submit(cmd).map should be(Map())
    }
  }

}
