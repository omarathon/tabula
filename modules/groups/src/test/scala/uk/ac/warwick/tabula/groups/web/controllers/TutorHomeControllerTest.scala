package uk.ac.warwick.tabula.groups.web.controllers

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.groups.commands.TutorHomeCommand
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel

class TutorHomeControllerTest extends TestBase {

	@Test def controllerShowsYourGroups() {
		val command = new TutorHomeCommand {
			def apply() = Map()
		}
		val controller = new TutorHomeController
		val mav = controller.listModules(command, null)

		mav.map("data") should be (GroupsViewModel.ViewModules(Nil, canManageDepartment=false))
	}

}
