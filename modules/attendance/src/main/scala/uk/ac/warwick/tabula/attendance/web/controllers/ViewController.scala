package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.AutowiringMonitoringPointServiceComponent
import scala.collection.JavaConverters._

@Controller
@RequestMapping(Array("/{department}"))
class ViewController extends AttendanceController with AutowiringMonitoringPointServiceComponent {

	@RequestMapping
	def home(@PathVariable department: Department) = {
		Mav("home/view",
			"department" -> mandatory(department),
			"hasSets" -> monitoringPointService.hasAnyPointSets(department)
		)
	}

}
