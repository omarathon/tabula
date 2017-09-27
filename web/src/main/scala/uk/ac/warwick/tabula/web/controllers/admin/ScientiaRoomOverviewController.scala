package uk.ac.warwick.tabula.web.controllers.admin

import uk.ac.warwick.tabula.services.timetables.ScientiaCentrallyManagedRooms
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.web.Mav

@Controller @RequestMapping(value = Array("/admin/scientia-rooms"))
class ScientiaRoomOverviewController extends AdminController {

	@RequestMapping
	def list(): Mav = {
		Mav(
			"admin/scientia/list",
			"rooms" -> ScientiaCentrallyManagedRooms.CentrallyManagedRooms.values.toSeq.sortBy(_.syllabusPlusName.getOrElse(""))
		)
	}

}
