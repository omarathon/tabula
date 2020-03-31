package uk.ac.warwick.tabula.api.web.controllers.timetables

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/v1/academicyear"))
class AcademicYearController extends ApiController {
  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def get(): Mav =
    Mav(new JSONView(Map(
      "success" -> true,
      "status" -> "ok",
      "academicYear" -> AcademicYear.now().toString()
    )))
}