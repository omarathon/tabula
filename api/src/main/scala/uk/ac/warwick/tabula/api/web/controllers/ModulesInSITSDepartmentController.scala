package uk.ac.warwick.tabula.api.web.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/v1/department/{departmentCode}/modules"))
class ModulesInSITSDepartmentController extends ApiController {

  @Autowired var moduleAndDepartmentService: ModuleAndDepartmentService = _

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def index(@PathVariable departmentCode: String): Mav = {
    Mav(new JSONView(
      Map(
        "success" -> true,
        "status" -> "ok",
        "modules" -> moduleAndDepartmentService.findModulesBySITSDepartmentCode(departmentCode).map(r => Map(
          "code" -> r.code,
          "name" -> r.name,
        ))
      )
    ))
  }

}
