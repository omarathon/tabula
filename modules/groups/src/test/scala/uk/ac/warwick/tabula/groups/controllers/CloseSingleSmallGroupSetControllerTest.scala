package uk.ac.warwick.tabula.groups.controllers

import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.groups.web.controllers.admin.OpenSmallGroupSetController
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroupSet}
import uk.ac.warwick.tabula.commands.Appliable

class CloseSingleSmallGroupSetControllerTest extends TestBase with Mockito {
	
	val controller = new OpenSmallGroupSetController
	val set = new SmallGroupSet
	set.openForSignups = true //set is already open.
	set.allocationMethod = SmallGroupAllocationMethod.StudentSignUp


	@Test
	def createsCommand() {
		withUser("test") {
			val command = controller.getOpenGroupSetCommand(set,"close")
			command.singleSetToOpen should be(set)
		}
	}

	@Test
	def showsForm() {
		val mockCommand = mock[Appliable[Seq[SmallGroupSet]]]
		controller.form(mockCommand).viewName should be("admin/groups/open")
	}



}
