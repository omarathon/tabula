package uk.ac.warwick.tabula.profiles.web.controllers.admin

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.actions.Manage
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.profiles.commands.UploadPersonalTutorsCommand
import uk.ac.warwick.tabula.data.model.Department
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.helpers.Logging

@Controller
@RequestMapping(value = Array("/admin/department/{department}/tutors"))
class UploadPersonalTutorsController extends ProfilesController {

	@ModelAttribute def command = new UploadPersonalTutorsCommand

	@RequestMapping(method = Array(HEAD, GET))
	def uploadForm(@PathVariable department: Department, @ModelAttribute cmd: UploadPersonalTutorsCommand): Mav = {
		mustBeAbleTo(Manage(department))

//		var marksToDisplay = members.map { member => 
//			val feedback = assignmentService.getStudentFeedback(assignment, member.getWarwickId)
//			noteMarkItem(member, feedback)
//		}
//
		Mav("admin/department/tutors/uploadform") //, "marksToDisplay" -> marksToDisplay)
	}

	@RequestMapping(method = Array(POST), params = Array("!confirm"))
	def confirmBatchUpload(@PathVariable department: Department, @ModelAttribute cmd: UploadPersonalTutorsCommand, errors: Errors): Mav = {
		bindAndValidate(department, cmd, errors)
		Mav("admin/department/tutors/uploadpreview")
	}

	@RequestMapping(method = Array(POST), params = Array("confirm=true"))
	def doUpload(@PathVariable department: Department, @ModelAttribute cmd: UploadPersonalTutorsCommand, errors: Errors): Mav = {
		bindAndValidate(department, cmd, errors)
		cmd.apply()
		Mav("admin/department/tutors/uploadform")
	}

	private def bindAndValidate(department: Department, cmd: UploadPersonalTutorsCommand, errors: Errors) {
		//mustBeLinked(cmd.assignment, module)
		mustBeAbleTo(Manage(department))
		cmd.onBind
		cmd.postExtractValidation(errors, department)
	}
}