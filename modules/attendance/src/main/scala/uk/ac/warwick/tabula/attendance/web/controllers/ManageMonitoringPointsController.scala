package uk.ac.warwick.tabula.attendance.web.controllers

import scala.collection.JavaConverters.asScalaBufferConverter
import org.hibernate.annotations.AccessType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import javax.persistence.Entity
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.attendance.commands.GetMonitoringPointsCommand
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.JSONView
import org.json.JSONObject
import org.json.JSONArray

@Controller
@RequestMapping(Array("/manage/{dept}"))
class ManageMonitoringPointsController extends BaseController {

  private def toInt(text: String) =
    if (text.hasText) try { Some(text.toInt) } catch { case e: NumberFormatException => None }
    else None

  @RequestMapping(method = Array(GET, HEAD))
  def render(@PathVariable("dept") dept: Department, user: CurrentUser) =
    Mav("home/manage", "department" -> dept)

  @RequestMapping(value = Array("/points"), method = Array(GET, HEAD))
  def getPoints(@RequestParam route: Route, @RequestParam(value = "year", required = false) year: String) = {
    val mps = GetMonitoringPointsCommand(mandatory(route), toInt(year)).apply
    mps map { pointSet =>
      val points = pointSet.points.asScala map { point =>
        Map(
          "id" -> point.id,
          "name" -> point.name,
          "defaultValue" -> point.defaultValue,
          "week" -> point.week)
      }

      Map(
        "points" -> points)
    } match {
      case Some(model) => new JSONView(model)
      case _ => new JSONView(Map())
    }
  }

}