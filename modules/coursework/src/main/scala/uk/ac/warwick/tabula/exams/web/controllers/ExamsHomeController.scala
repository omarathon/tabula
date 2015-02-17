package uk.ac.warwick.tabula.exams.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.coursework.commands.assignments.AddAssignmentCommand
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.exams.commands.AddExamCommand
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.CurrentUser
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}

@Controller
@RequestMapping(Array("/exams/"))
class ExamsHomeController extends BaseController {

	@RequestMapping
	def examsHome(user: CurrentUser) = Mav("exams/home/view")
}
