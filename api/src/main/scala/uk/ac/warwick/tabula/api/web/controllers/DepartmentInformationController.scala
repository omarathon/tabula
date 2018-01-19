package uk.ac.warwick.tabula.api.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.AutowiringModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

import scala.collection.JavaConverters._

trait DepartmentInformationController extends ApiController {
	def jsonDepartmentObject(department: Department, subDepartments: Boolean = false): Map[String, Any] =
		Seq(
			Some("code" -> department.code.toUpperCase),
			Some("fullName" -> department.fullName),
			Some("name" -> department.name),
			Some("shortName" -> department.shortName),
			Some("modules" -> department.modules.asScala.sorted.map { module =>
				Map(
					"code" -> module.code.trim.toUpperCase,
					"name" -> module.name
				)
			}),
			Some("routes" -> department.routes.asScala.sorted.map { route =>
				Map(
					"code" -> route.code.trim.toUpperCase,
					"name" -> route.name,
					"degreeType" -> route.degreeType.description
				)
			}),
			Option(department.parent).map { parent => "parentDepartment" -> Map(
				"code" -> parent.code.toUpperCase,
				"name" -> parent.name
			)},
			Option(department.filterRule).filterNot(_ == Department.AllMembersFilterRule).map { rule => "filter" -> rule.name },
			if (subDepartments && !department.children.isEmpty)
				Some("subDepartments" -> department.children.asScala.toSeq.sortBy(_.code).map(jsonDepartmentObject(_, subDepartments = true)))
			else None
		).flatten.toMap
}

@Controller
@RequestMapping(Array("/v1/department"))
class ListDepartmentsController extends DepartmentInformationController
	with AutowiringModuleAndDepartmentServiceComponent {

	@ModelAttribute("departments")
	def departments: Seq[Department] = moduleAndDepartmentService.allDepartments

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def list(@ModelAttribute("departments") departments: Seq[Department]): Mav =
		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok",
			"departments" -> departments.map(jsonDepartmentObject(_))
		)))

}

@Controller
@RequestMapping(Array("/v1/department/{department}"))
class GetDepartmentInformationController extends DepartmentInformationController {

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def get(@PathVariable department: Department): Mav =
		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok",
			"department" -> jsonDepartmentObject(mandatory(department), subDepartments = true)
		)))

}