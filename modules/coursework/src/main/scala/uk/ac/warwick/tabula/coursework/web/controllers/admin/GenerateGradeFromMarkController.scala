package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{RequestParam, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.coursework.commands.feedback.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{GradeBoundary, Assignment, Module}
import uk.ac.warwick.tabula.JavaImports._
import collection.JavaConverters._

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/generate-grade"))
class GenerateGradeFromMarkController extends CourseworkController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		GenerateGradesFromMarkCommand(mandatory(module), mandatory(assignment))

	@RequestMapping(method= Array(POST))
	def post(
		@ModelAttribute("command") cmd: Appliable[Map[String, Seq[GradeBoundary]]],
		errors: Errors,
		@RequestParam(value = "selected", required = false) selected: String
	) = {
		val result = cmd.apply().values.toSeq.headOption.getOrElse(Seq()).sorted
		val default = {
			if (selected == null) {
				result.find(_.isDefault)
			} else {
				result.find(_.grade == selected)
			}
		}

		Mav("admin/_generatedGrades",
			"grades" -> result,
			"default" -> default
		).noLayout()
	}

	@RequestMapping(value = Array("/multiple"), method= Array(POST))
	def postMultiple(@ModelAttribute("command") cmd: Appliable[Map[String, Seq[GradeBoundary]]], @RequestParam(value = "selected", required = false) selected: JMap[String, String]) = {
		val result = cmd.apply()
		val defaults = result.map{case(universityId, grades) => universityId -> {
			if (selected == null) {
				grades.find(_.isDefault)
			} else {
				selected.asScala.find(_._1 == universityId) match {
					case Some((_, selectedGrade)) => grades.find(_.grade == selected)
						case _ => grades.find(_.isDefault)
				}
			}
		}}

		Mav("admin/generatedGrades",
			"result" -> result,
			"defaults" -> defaults
		).noLayout()
	}

}
