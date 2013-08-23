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
import scala.collection.JavaConverters._
import org.springframework.scala.jdbc.core.JdbcCallbackConversions
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSet

/**
 * Displays the screen for selecting a route+year and displaying the monitoring
 * points for that combination (the monitoring point set).
 */
@Controller
@RequestMapping(Array("/manage/{dept}"))
class ManageMonitoringPointsController extends AttendanceController {

	@RequestMapping
	def home(@PathVariable("dept") dept: Department) = {
		val academicYear = AcademicYear.guessByDate(new DateTime())
		val pointSetsForThisYearByRoute = dept.routes.asScala.collect{
			case r: Route => r.monitoringPointSets.asScala.filter(s => s.academicYear.equals(academicYear))
		}.flatten.groupBy(_.route)
		val routes = pointSetsForThisYearByRoute.keySet
		Mav("manage/home",
			"department" -> dept,
			"setCount" -> dept.routes.asScala.map{r => r.monitoringPointSets.asScala.filter(s => s.academicYear.equals(academicYear)).size}.sum,
			"academicYear" -> academicYear,
			"routesWithSets" -> routes,
			"pointSetsForThisYearByRoute" -> pointSetsForThisYearByRoute.map{case (r, s) => r.code -> s}
		)
	}

}