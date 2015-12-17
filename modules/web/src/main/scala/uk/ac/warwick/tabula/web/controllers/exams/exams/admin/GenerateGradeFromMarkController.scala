package uk.ac.warwick.tabula.web.controllers.exams.exams.admin

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.feedback.{GenerateGradesFromMarkCommand, GenerateGradesFromMarkCommandState}
import uk.ac.warwick.tabula.data.model.{Exam, GradeBoundary, Module}
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController

import scala.collection.JavaConverters._

abstract class AbstractGenerateGradeFromMarkController extends ExamsController {

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

		Mav("exams/exams/admin/_generatedGrades",
			"grades" -> result,
			"default" -> default
		).noLayout()
	}

	@RequestMapping(value = Array("/multiple"), method= Array(POST))
	def postMultiple(@ModelAttribute("command") cmd: Appliable[Map[String, Seq[GradeBoundary]]] with GenerateGradesFromMarkCommandState) = {
		val result = cmd.apply()
		val defaults = result.map{case(universityId, grades) => universityId -> {
				cmd.selected.asScala.find(_._1 == universityId) match {
					case Some((_, selectedGrade)) => grades.find(_.grade == selectedGrade)
					case _ => None
				}
			}
		}

		Mav("exams/exams/admin/generatedGrades",
			"result" -> result,
			"defaults" -> defaults
		).noLayout()
	}

}

@Controller
@RequestMapping(Array("/exams/exams/admin/module/{module}/exams/{exam}/generate-grade"))
class GenerateExamGradeFromMarkController extends AbstractGenerateGradeFromMarkController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable exam: Exam) =
		GenerateGradesFromMarkCommand(mandatory(module), mandatory(exam))

}
