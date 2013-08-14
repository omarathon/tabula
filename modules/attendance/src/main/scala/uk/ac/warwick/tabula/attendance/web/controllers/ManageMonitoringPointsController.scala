package uk.ac.warwick.tabula.attendance.web.controllers

import scala.collection.JavaConverters._

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.tabula.JavaImports
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.attendance.commands.GetMonitoringPointsCommand
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.JSONView

/**
 * Displays the screen for selecting a route+year and displaying the monitoring
 * points for that combination (the monitoring point set).
 */
@Controller
@RequestMapping(Array("/manage/{dept}"))
class ManageMonitoringPointsController extends AttendanceController {

  private def toInt(text: String) =
    if (text.hasText) try { Some(text.toInt) } catch { case e: NumberFormatException => None }
    else None

  @RequestMapping(method = Array(GET, HEAD))
  def render(@PathVariable("dept") dept: Department, user: CurrentUser) =
    Mav("manage/list", "department" -> dept)

	/*def getPoints(@RequestParam route: Route, @RequestParam year: JInteger) = {

	}*/

	/*
		TODO think this should render HTML, else we'll be converting JSON to HTML in JS.
	 */
	@RequestMapping(value = Array("/points.json"), method = Array(GET, HEAD))
	def getPoints(@RequestParam route: Route, @RequestParam(value = "year", required = false) year: String) = {
		// for-comprehension on a bunch of Options is a neat way to say "do this if all options are present,
		// otherwise return None" without having to handle each one individually
		val pointSetOption = for {
			r <- Option(route)
			result <- GetMonitoringPointsCommand(route, toInt(year)).apply
		} yield result

		val model = pointSetOption map { pointSet =>
			Map(
				"templateName" -> pointSet.templateName,
				"year" -> pointSet.year,
				"points" -> (for (point <- pointSet.points.asScala) yield Map(
					"id" -> point.id,
					"name" -> point.name,
					"defaultValue" -> point.defaultValue,
					"week" -> point.week
				))
			)
		} getOrElse {
			Map()
		}

		new JSONView(model)
	}

}