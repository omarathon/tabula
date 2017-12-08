package uk.ac.warwick.tabula.web.controllers.ajax

import org.joda.time.LocalDate
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam}
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.JSONView

import scala.util.{Failure, Success, Try}

/**
	* Returns the start date (inclusive) and end date (exclusive, the first datetime NOT in the provided academic year)
	* of the requested academic year
	*/
@Controller
@RequestMapping(Array("/ajax/academicyearfromdate"))
class AcademicYearFromDateController extends BaseController {

	@RequestMapping
	def result(@RequestParam date: LocalDate): Mav =
		Try(AcademicYear.forDate(date)) match {
			case Success(academicYear) => Mav(new JSONView(Map(
				"startYear" -> academicYear.startYear,
				"string" -> academicYear.toString
			)))
			case Failure(e) => throw new ItemNotFoundException(date, s"Couldn't find an academic year for $date: $e")
		}

}
