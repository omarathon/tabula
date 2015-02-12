package uk.ac.warwick.tabula.exams.web.controllers

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.web.controllers.BaseController


@Controller("examsHomeController")
class HomeController extends BaseController {

	@RequestMapping(Array("/")) def examsHome() = {
		Mav("exams/home/view")
	}
}
