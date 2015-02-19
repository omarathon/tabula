package uk.ac.warwick.tabula.exams.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.CurrentUser

@Controller
@RequestMapping(Array("/exams/"))
class ExamsHomeController extends ExamsController {

	@RequestMapping
	def examsHome(user: CurrentUser) = Mav("exams/home/view")
}
