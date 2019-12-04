package uk.ac.warwick.tabula.api.web.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.AssessmentComponent
import uk.ac.warwick.tabula.services.AssessmentMembershipService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/v1/module/{moduleCode}/occurrences/{academicYear}"))
class ModuleOccurrencesController extends ApiController {

  @Autowired var assessmentMembershipService: AssessmentMembershipService = _

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def index(@PathVariable moduleCode: String, @PathVariable academicYear: AcademicYear): Mav = {
    Mav(new JSONView(
      Map(
        "success" -> true,
        "status" -> "ok",
        "occurrences" -> assessmentMembershipService.getUpstreamAssessmentGroups(academicYear, moduleCode).map(_.occurrence).distinct.filterNot(_ == AssessmentComponent.NoneAssessmentGroup)
      )
    ))
  }

}
