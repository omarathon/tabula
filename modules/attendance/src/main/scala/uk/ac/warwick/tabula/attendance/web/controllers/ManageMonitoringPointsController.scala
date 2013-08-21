package uk.ac.warwick.tabula.attendance.web.controllers


import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.attendance.commands.GetMonitoringPointsCommand
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Route
import org.joda.time.DateTime

/**
 * Displays the screen for selecting a route+year and displaying the monitoring
 * points for that combination (the monitoring point set).
 */
@Controller
@RequestMapping(Array("/manage/{dept}"))
class ManageMonitoringPointsController extends AttendanceController {

	private def toInt(text: String) =
		if (text.hasText) try {
			Some(text.toInt)
		} catch {
			case e: NumberFormatException => None
		}
		else None

	@RequestMapping(method = Array(GET, HEAD))
	def render(@PathVariable("dept") dept: Department, user: CurrentUser) =
		Mav("manage/list", "department" -> dept)

	@RequestMapping(value = Array("/years"), method = Array(GET, HEAD))
	def getAcademicYears(@PathVariable("dept") dept: Department, @RequestParam route: Route) =
		Mav("manage/academicYears",
			"route" -> route,
			"departmentCode" -> dept.code,
			"academicYear" -> AcademicYear.guessByDate(new DateTime())
		).noLayout()

	@RequestMapping(value = Array("/sets"), method = Array(GET, HEAD))
	def getSets(@PathVariable("dept") dept: Department, @RequestParam route: Route) =
		Mav("manage/sets", "route" -> route, "departmentCode" -> dept.code).noLayout()

	@RequestMapping(value = Array("/points"), method = Array(GET, HEAD))
	def getPoints(@PathVariable("dept") dept: Department, @RequestParam route: Route, @RequestParam(value = "year", required = false) year: String) = {
		// for-comprehension on a bunch of Options is a neat way to say "do this if all options are present,
		// otherwise return None" without having to handle each one individually
		val pointSetOption = for {
			r <- Option(route)
			result <- GetMonitoringPointsCommand(route, toInt(year)).apply
		} yield result

		val mav = pointSetOption map {
			pointSetPair => {
				Mav("manage/points", "route" -> route, "departmentCode" -> dept.code,
					"pointSet" -> pointSetPair._1, "pointsByTerm" -> pointSetPair._2).noLayout()
			}
		} getOrElse {
			Mav("manage/sets", "route" -> route, "departmentCode" -> dept.code).noLayout()
		}

		mav
	}

}