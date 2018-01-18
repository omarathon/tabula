package uk.ac.warwick.tabula.api.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.services.AutowiringModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

trait ModuleInformationController extends ApiController {
	def jsonModuleObject(module: Module): Map[String, Any] =
		Map(
			"code" -> module.code.toUpperCase,
			"name" -> module.name,
			"active" -> module.active,
			"adminDepartment" -> Map(
				"code" -> module.adminDepartment.code.toUpperCase,
				"name" -> module.adminDepartment.name
			)
		)
}

@Controller
@RequestMapping(Array("/v1/module"))
class ListModulesController extends ModuleInformationController
	with AutowiringModuleAndDepartmentServiceComponent {

	@ModelAttribute("modules")
	def modules: Seq[Module] = moduleAndDepartmentService.allModules

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def list(@ModelAttribute("modules") modules: Seq[Module]): Mav =
		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok",
			"modules" -> modules.map(jsonModuleObject)
		)))

}

@Controller
@RequestMapping(Array("/v1/module/{module}"))
class GetModuleInformationController extends ModuleInformationController {

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def get(@PathVariable module: Module): Mav =
		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok",
			"module" -> jsonModuleObject(mandatory(module))
		)))

}
