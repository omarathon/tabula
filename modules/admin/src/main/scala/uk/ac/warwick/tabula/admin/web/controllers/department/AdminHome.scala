package uk.ac.warwick.tabula.admin.web.controllers.department

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.admin.web.Routes
import uk.ac.warwick.tabula.admin.web.controllers.AdminController

/**
 * Screens for department and module admins.
 */
@Controller
@RequestMapping(Array("/department"))
class AdminHomeController extends AdminController {
	@RequestMapping
	def homeScreen(user: CurrentUser) = Redirect(Routes.home)
}