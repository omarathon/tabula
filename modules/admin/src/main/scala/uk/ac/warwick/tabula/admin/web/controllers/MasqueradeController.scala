package uk.ac.warwick.tabula.admin.web.controllers

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute

import org.springframework.web.bind.annotation.RequestMapping

import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.admin.commands.MasqueradeCommand
import uk.ac.warwick.tabula.web.Cookies._
import uk.ac.warwick.tabula.web.{Routes, Cookie, Mav}
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.services.{CourseAndRouteService, ProfileService, ModuleAndDepartmentService}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.{Route, Member}
import uk.ac.warwick.tabula.sandbox.SandboxData
import uk.ac.warwick.tabula.CurrentUser

@Controller
@RequestMapping(Array("/masquerade"))
class MasqueradeController extends AdminController {

	var sandbox = (Wire.property("${spring.profiles.active}") == "sandbox")
	var departmentService = Wire[ModuleAndDepartmentService]
	var profileService = Wire[ProfileService]
	var routeService = Wire[CourseAndRouteService]

	validatesSelf[SelfValidating]

	type MasqueradeCommand = Appliable[Option[Cookie]]

	@ModelAttribute("masqueradeCommand") def command(): MasqueradeCommand = MasqueradeCommand(user)

	@RequestMapping(method = Array(HEAD, GET))
	def form(@ModelAttribute("masqueradeCommand") cmd: MasqueradeCommand): Mav = Mav("masquerade/form").crumbs(Breadcrumbs.Current("Masquerade"))

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("masqueradeCommand") cmd: MasqueradeCommand, errors: Errors, response: HttpServletResponse): Mav = {
		if (errors.hasErrors()) form(cmd)
		else {
			for (cookie <- cmd.apply()) response.addCookie(cookie)
			Redirect(Routes.admin.masquerade)
		}
	}

	@ModelAttribute("masqueradeDepartments") def masqueradeDepartments =
		if (!sandbox) Nil
		else {
			val realUser = new CurrentUser(user.realUser, user.realUser)

			departmentService.departmentsWithPermission(realUser, Permissions.Masquerade).map { department =>
				val sandboxDept = SandboxData.Departments(department.code)
				val students = sandboxDept.routes.values.toSeq.flatMap { route =>
					routeService.getRouteByCode(route.code).map { r =>
						(r,(route.studentsStartId until math.min(route.studentsStartId + 3, route.studentsEndId)).flatMap { id =>
								profileService.getMemberByUniversityId(id.toString, disableFilter = true).map { MasqueradableUser(_) }
							})
					}
				}
				val staff = (sandboxDept.staffStartId until math.min(sandboxDept.staffStartId + 10, sandboxDept.staffEndId)).flatMap { id =>
					profileService.getMemberByUniversityId(id.toString, disableFilter = true).map { MasqueradableUser(_) }
				}

				MasqueradeDepartment(department.name, department.code, students, staff)
			}
		}

	@ModelAttribute("returnTo") def returnTo = getReturnTo("")

}

case class MasqueradeDepartment(
	name: String,
	code: String,
	students: Seq[(Route, Seq[MasqueradableUser])],
	staff: Seq[MasqueradableUser]
)

case class MasqueradableUser(
	userId: String,
	fullName: String
)
object MasqueradableUser {
	def apply(m: Member): MasqueradableUser = MasqueradableUser(m.userId, m.fullName.getOrElse(""))
}