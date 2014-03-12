package uk.ac.warwick.tabula.admin.web.controllers.department

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.admin.web.Routes

class AdminHomeControllerTest extends TestBase {

	val controller = new AdminHomeController

	@Test def redirect = withUser("cuscav") {
		controller.homeScreen(currentUser).viewName should be (s"redirect:${Routes.home}")
	}

}
