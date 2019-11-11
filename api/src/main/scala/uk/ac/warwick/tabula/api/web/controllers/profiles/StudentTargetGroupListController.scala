package uk.ac.warwick.tabula.api.web.controllers.profiles

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.data.model.MemberUserType
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/v1/studentTargetGroups"))
class StudentTargetGroupListController extends ApiController {


  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def index(): Mav = {
    Mav(new JSONView(
      Map(
        "success" -> true,
        "status" -> "ok",
        "groups" -> MemberUserType.StudentTargetGroups
      )
    ))
  }

}
