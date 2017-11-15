package uk.ac.warwick.tabula.web.controllers.ajax

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Course
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(value = Array("/ajax/coursepicker/query"))
class CoursePickerController extends BaseController {

	@ModelAttribute("command")
	def command = CoursePickerCommand()

	@RequestMapping
	def query(@ModelAttribute("command")cmd: Appliable[Seq[Course]]): Mav = {
		val courses = cmd.apply()
		Mav(
			new JSONView(
				courses.map(course => Map(
					"code" -> course.code,
					"name" -> course.name
				))
			)
		)
	}

}

class CoursePickerCommand extends CommandInternal[Seq[Course]] {

	self: CourseAndRouteServiceComponent =>

	var query: String = _

	def applyInternal(): Seq[Course] = {
		if (query.isEmpty) {
			Seq()
		} else {
			courseAndRouteService.findCoursesNamedLike(query)
		}
	}

}

object CoursePickerCommand {
	def apply() =
		new CoursePickerCommand with Command[Seq[Course]]
		with AutowiringCourseAndRouteServiceComponent
		with ReadOnly with Unaudited with Public
}
