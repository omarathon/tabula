package uk.ac.warwick.tabula.web.controllers.exams.grids.generate

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{PopulateOnForm, SelfValidating, Appliable}
import uk.ac.warwick.tabula.commands.exams.grids.GenerateExamGridOvercatCommand
import uk.ac.warwick.tabula.data.model.{Department, Module, StudentCourseYearDetails}
import uk.ac.warwick.tabula.services.AutowiringModuleRegistrationServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}

@Controller
@RequestMapping(Array("/exams/grids/{department}/{academicYear}/generate/overcatting/{scyd}"))
class OvercattingOptionsController extends ExamsController with AutowiringModuleRegistrationServiceComponent {

	validatesSelf[SelfValidating]

	@ModelAttribute("GenerateExamGridMappingParameters")
	def params = GenerateExamGridMappingParameters

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, @PathVariable scyd: StudentCourseYearDetails) =
		GenerateExamGridOvercatCommand(mandatory(department), mandatory(academicYear), mandatory(scyd), user)

	@RequestMapping(method = Array(GET))
	def form(@ModelAttribute("command") cmd: Appliable[Seq[Module]] with PopulateOnForm): Mav = {
		cmd.populate()
		Mav("exams/grids/generate/overcat").noLayoutIf(ajax)
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[Seq[Module]], errors: Errors) = {
		if (errors.hasErrors) {
			new JSONErrorView(errors)
		} else {
			new JSONView(Map(
				"modules" -> cmd.apply().map(_.code)
			))
		}
	}

}
