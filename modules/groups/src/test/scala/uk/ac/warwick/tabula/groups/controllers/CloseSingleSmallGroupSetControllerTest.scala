package uk.ac.warwick.tabula.groups.controllers

import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.groups.web.controllers.admin.OpenSmallGroupSetController
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroupSet}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSetSelfSignUpState
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Module

class CloseSingleSmallGroupSetControllerTest extends TestBase with Mockito {
	
	val controller = new OpenSmallGroupSetController
	val module = new Module
	val set = new SmallGroupSet
	set.openForSignups = true //set is already open.
	set.allocationMethod = SmallGroupAllocationMethod.StudentSignUp


	@Test
	def createsCommand() {
		withUser("test") {
			val command = controller.getOpenGroupSetCommand(module, set, SmallGroupSetSelfSignUpState.Closed)
			command.singleSetToOpen should be(set)
		}
	}

	@Test
	def showsForm() {
		val mockCommand = mock[Appliable[Seq[SmallGroupSet]]]
		controller.form(mockCommand).viewName should be("admin/groups/open")
	}



}
