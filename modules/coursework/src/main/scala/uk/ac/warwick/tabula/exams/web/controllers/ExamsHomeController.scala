package uk.ac.warwick.tabula.exams.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.web.controllers.BaseController


@Controller()
class ExamsHomeController extends BaseController {

	@RequestMapping(Array("/exams")) def examsHome() = {
		Mav("exams/home/view")
	}
}
