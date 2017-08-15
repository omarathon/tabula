package uk.ac.warwick.tabula.api.web.controllers.profiles

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands.ViewViewableCommand
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/v1/department/{department}/usersearch"))
class DepartmentUserSearchController extends ApiController
	with GetDepartmentUsersApi with AutowiringProfileServiceComponent {

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def all(
		@ModelAttribute("getCommand") command: ViewViewableCommand[Department],
		@PathVariable department: Department
	): Mav = {
		getMav(profileService.findUsercodesInHomeDepartment(department))
	}

	@RequestMapping(path = Array("/teachingstaff"), method = Array(GET), produces = Array("application/json"))
	def teachingStaff(
		@ModelAttribute("getCommand") command: ViewViewableCommand[Department],
		@PathVariable department: Department
	): Mav = {
		getMav(profileService.findTeachingStaffUsercodesInHomeDepartment(department))
	}

	@RequestMapping(path = Array("/adminstaff"), method = Array(GET), produces = Array("application/json"))
	def adminStaff(
		@ModelAttribute("getCommand") command: ViewViewableCommand[Department],
		@PathVariable department: Department
	): Mav = {
		getMav(profileService.findAdminStaffUsercodesInHomeDepartment(department))
	}

	@RequestMapping(path = Array("/undergraduates"), method = Array(GET), produces = Array("application/json"))
	def undergraduates(
		@ModelAttribute("getCommand") command: ViewViewableCommand[Department],
		@PathVariable department: Department
	): Mav = {
		getMav(profileService.findUndergraduatesUsercodesInHomeDepartment(department))
	}

	@RequestMapping(path = Array("/pgt"), method = Array(GET), produces = Array("application/json"))
	def pgt(
		@ModelAttribute("getCommand") command: ViewViewableCommand[Department],
		@PathVariable department: Department
	): Mav = {
		getMav(profileService.findTaughtPostgraduatesUsercodesInHomeDepartment(department))
	}

	@RequestMapping(path = Array("/pgr"), method = Array(GET), produces = Array("application/json"))
	def pgr(
		@ModelAttribute("getCommand") command: ViewViewableCommand[Department],
		@PathVariable department: Department
	): Mav = {
		getMav(profileService.findResearchPostgraduatesUsercodesInHomeDepartment(department))
	}
}


trait GetDepartmentUsersApi {

	self: ApiController with ProfileServiceComponent =>

	@ModelAttribute("getCommand")
	def getCommand(@PathVariable department: Department): ViewViewableCommand[Department] =
		new ViewViewableCommand(Permissions.Profiles.ViewSearchResults, mandatory(department))

	def getMav(usercodes: Seq[String]): Mav = {
		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok",
			"usercodes" -> usercodes
		)))
	}
}
