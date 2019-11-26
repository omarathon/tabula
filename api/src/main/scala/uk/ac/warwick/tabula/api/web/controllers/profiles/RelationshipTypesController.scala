package uk.ac.warwick.tabula.api.web.controllers.profiles

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/v1/relationships"))
class RelationshipTypesController extends ApiController {

  @Autowired var relationshipsService: RelationshipService = _

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def index(): Mav = {
    Mav(new JSONView(
      Map(
        "success" -> true,
        "status" -> "ok",
        "relationships" -> relationshipsService.allStudentRelationshipTypes.map(r => Map(
          "id" -> r.id,
          "urlPart" -> r.urlPart,
          "description" -> r.description,
          "agentRole" -> r.agentRole,
          "studentRole" -> r.studentRole
        ))
      )
    ))
  }

}
