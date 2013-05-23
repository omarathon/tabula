package uk.ac.warwick.tabula.profiles.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.profiles.commands.UploadPersonalTutorsCommand
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.system.BindListener
import org.hibernate.validator.Valid
import collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import org.springframework.validation.ObjectError

@Controller
@RequestMapping(value = Array("/department/{department}/tutors/upload"))
class UploadPersonalTutorsController extends ProfilesController {

	validatesSelf[UploadPersonalTutorsCommand]

	@ModelAttribute("command") def command(@PathVariable("department") department: Department) = new UploadPersonalTutorsCommand(department)

	@RequestMapping(method = Array(HEAD, GET))
	def uploadForm(@PathVariable("department") department: Department, @ModelAttribute("command") cmd: UploadPersonalTutorsCommand): Mav = {
		Mav("tutors/upload_form")
	}

	@RequestMapping(method = Array(POST), params = Array("!confirm"))
	def confirmBatchUpload(@PathVariable("department") department: Department, @Valid @ModelAttribute("command") cmd: UploadPersonalTutorsCommand, errors: Errors): Mav = {
		if (errors.hasErrors && errors.getFieldError.getCode == "file.wrongtype.one") {
			uploadForm(department, cmd)
		} else {
			Mav("tutors/upload_preview")
		}
	}

	@RequestMapping(method = Array(POST), params = Array("confirm=true"))
	def doUpload(@PathVariable("department") department: Department, @Valid @ModelAttribute("command") cmd: UploadPersonalTutorsCommand, errors: Errors): Mav = {

		def isPersonSpecificError(err: ObjectError) = (err.getCode.contains("uniNumber") || err.getCode().contains("member"))

		val foundErrorsToDisplay = errors.getAllErrors().asScala.exists {
			!isPersonSpecificError(_)
		}

		val tutorCount = cmd.apply().size

		if (foundErrorsToDisplay) {
			Mav("tutors/upload_form",
					"tutorCount" -> tutorCount,
					"showErrors" -> true)
		}
		else {
			Mav("tutors/upload_form", "tutorCount" -> tutorCount)
		}
	}
}
