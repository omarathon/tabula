package uk.ac.warwick.tabula.web.controllers.attendance.view.old

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.AutowiringMonitoringPointServiceComponent
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController

@Controller
@RequestMapping(Array("/attendance/{department}"))
class ViewController extends AttendanceController with AutowiringMonitoringPointServiceComponent {

	@RequestMapping
	def home(@PathVariable department: Department) = {
		Mav("attendance/home/view",
			"department" -> mandatory(department),
			"hasSets" -> monitoringPointService.hasAnyPointSets(department)
		)
	}

}
