package uk.ac.warwick.tabula.groups.controllers

import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.groups.web.controllers.admin.OpenSmallGroupSetController
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroupSet}
import uk.ac.warwick.tabula.commands.Appliable

class OpenSingleSmallGroupSetControllerTest extends TestBase with Mockito {
	val controller = new OpenSmallGroupSetController
	val set = new SmallGroupSet
	set.allocationMethod = SmallGroupAllocationMethod.StudentSignUp


	@Test
	def createsCommand() {
		withUser("test") {
			val command = controller.getOpenGroupSetCommand(set)
			command.singleSetToOpen should be(set)
		}
	}

	@Test
	def showsForm() {
		val mockCommand = mock[Appliable[Seq[SmallGroupSet]]]
		controller.form(mockCommand).viewName should be("admin/groups/open")
	}

	@Test
	def submitCallsApply() {
		val mockCommand = mock[Appliable[Seq[SmallGroupSet]]]
		controller.submit(mockCommand)
		there was one(mockCommand).apply()
	}

	@Test
	def submitShowsSuccessView() {
		val mockCommand = mock[Appliable[Seq[SmallGroupSet]]]
		controller.submit(mockCommand).viewName should be ("ajax_success")
	}

}
