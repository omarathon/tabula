package uk.ac.warwick.tabula.home.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

import javax.validation.Valid
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.home.commands.departments.AddDeptOwnerCommand
import uk.ac.warwick.tabula.home.commands.departments.RemoveDeptOwnerCommand
import uk.ac.warwick.tabula.web.Mav

abstract class BaseDeptOwnerController extends BaseSysadminController {

	def redirectToDeptOwners(deptcode: String) = Mav("redirect:/sysadmin/departments/" + deptcode + "/owners/")

	def viewDepartmentOwners(@PathVariable dept: Department): Mav =
		Mav("sysadmin/departments/owners",
			"department" -> dept,
			"owners" -> dept.owners)
	
}

@Controller
@RequestMapping(Array("/sysadmin/departments/"))
class DeptOwnerController extends BaseDeptOwnerController {

	@RequestMapping
	def departments = Mav("sysadmin/departments/list",
		"departments" -> moduleService.allDepartments)

	@RequestMapping(Array("/{dept}/"))
	def department(@PathVariable dept: Department) = {
		Mav("sysadmin/departments/single",
			"department" -> dept)
	}

	@RequestMapping(value = Array("/{dept}/owners/"), method = Array(GET))
	def departmentOwners(@PathVariable dept: Department) = viewDepartmentOwners(dept)
	
}

@Controller
@RequestMapping(Array("/sysadmin/departments/{dept}/owners/delete"))
class RemoveDeptOwnerController extends BaseDeptOwnerController {
	@ModelAttribute("removeOwner") def addOwnerForm(@PathVariable("dept") dept: Department) = {
		new RemoveDeptOwnerCommand(dept)
	}

	@RequestMapping(method = Array(POST))
	def addDeptOwner(@PathVariable dept: Department, @Valid @ModelAttribute("removeOwner") form: RemoveDeptOwnerCommand, errors: Errors) = {
		if (errors.hasErrors) {
			viewDepartmentOwners(dept)
		} else {
			logger.info("Passed validation, removing owner")
			form.apply()
			redirectToDeptOwners(dept.code)
		}
	}
}

@Controller
@RequestMapping(Array("/sysadmin/departments/{dept}/owners/add"))
class AddDeptOwnerController extends BaseDeptOwnerController {

	validatesWith { (cmd: AddDeptOwnerCommand, errors: Errors) =>
		if (cmd.getUsercodes.contains(cmd.usercode)) {
			errors.rejectValue("usercode", "userId.duplicate")
		} else if (!userLookup.getUserByUserId(cmd.usercode).isFoundUser) {
			errors.rejectValue("usercode", "userId.notfound")
		}
	}

	@ModelAttribute("addOwner") def addOwnerForm(@PathVariable("dept") dept: Department) = {
		new AddDeptOwnerCommand(dept)
	}

	@RequestMapping(method = Array(GET))
	def showForm(@PathVariable dept: Department, @ModelAttribute("addOwner") form: AddDeptOwnerCommand, errors: Errors) = {
		Mav("sysadmin/departments/owners/add",
			"department" -> dept)
	}

	@RequestMapping(method = Array(POST))
	def submit(@PathVariable dept: Department, @Valid @ModelAttribute("addOwner") form: AddDeptOwnerCommand, errors: Errors) = {
		if (errors.hasErrors) {
			showForm(dept, form, errors)
		} else {
			logger.info("Passed validation, saving owner")
			form.apply()
			redirectToDeptOwners(dept.code)
		}
	}
}