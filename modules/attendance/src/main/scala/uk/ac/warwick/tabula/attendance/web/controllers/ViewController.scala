package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.Department

@Controller
@RequestMapping(Array("/{department}"))
class ViewController extends AttendanceController {

	@RequestMapping
	def home(@PathVariable department: Department) = {
		Mav("home/view", "department" -> mandatory(department))
	}

}
