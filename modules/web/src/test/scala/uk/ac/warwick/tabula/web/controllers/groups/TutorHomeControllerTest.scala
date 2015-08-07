package uk.ac.warwick.tabula.web.controllers.groups

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.commands.groups.TutorHomeCommand
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel

class TutorHomeControllerTest extends TestBase {

	@Test def controllerShowsYourGroups() {
		val command = new TutorHomeCommand {
			def apply() = Map()
		}
		val controller = new GroupsTutorHomeController
		val mav = controller.listModules(command, null)

		mav.map("data") should be (GroupsViewModel.ViewModules(Nil, canManageDepartment=false))
	}

}
