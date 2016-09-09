package uk.ac.warwick.tabula.web.controllers.ajax

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.JSONView

/**
	* Returns the start date (inclusive) and end date (exclusive, the first datetime NOT in the provided academic year)
	* of the requested academic year
	*/
@Controller
@RequestMapping(Array("/ajax/academicyearfromdate"))
class AcademicYearFromDateController extends BaseController with AutowiringTermServiceComponent {

	@RequestMapping
	def result(@RequestParam date: DateTime): Mav = {
		val academicYear = AcademicYear.findAcademicYearContainingDate(date)(termService)
		Mav(new JSONView(Map(
			"startYear" -> academicYear.startYear,
			"string" -> academicYear.toString
		)))
	}

}
