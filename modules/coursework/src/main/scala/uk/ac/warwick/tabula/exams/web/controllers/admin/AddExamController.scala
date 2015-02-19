package uk.ac.warwick.tabula.exams.web.controllers.admin


import uk.ac.warwick.tabula.exams.web.controllers.ExamsController

import scala.collection.JavaConverters._
import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.{Exam, Module}
import uk.ac.warwick.tabula.exams.commands.{AddExamCommandState, AddExamCommand}
import uk.ac.warwick.tabula.web.controllers.BaseController
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.coursework.web.Routes

@Controller
@RequestMapping(value = Array("/exams/admin/module/{module}/exam/new"))
class AddExamController extends ExamsController {

	type AddExamCommand = Appliable[Exam] with AddExamCommandState

	validatesSelf[SelfValidating]

	@ModelAttribute("academicYearChoices") def academicYearChoices: JList[AcademicYear] = {
		AcademicYear.guessSITSAcademicYearByDate(DateTime.now).yearsSurrounding(2, 2).asJava
	}

	@ModelAttribute("command")
	def command(@PathVariable("module") module: Module) = AddExamCommand(mandatory(module))

	@RequestMapping(method = Array(HEAD, GET))
	def showForm(@ModelAttribute("command") cmd: AddExamCommand) = Mav("exams/admin/new")

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: AddExamCommand, errors: Errors) = {
		if (errors.hasErrors) {
			showForm(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.home)
		}
	}

}
