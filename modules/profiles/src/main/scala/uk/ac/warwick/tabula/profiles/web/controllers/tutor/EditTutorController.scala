package uk.ac.warwick.tabula.profiles.web.controllers.tutor

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

import javax.servlet.http.HttpServletRequest
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.profiles.commands.tutor.EditTutorCommand
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(Array("/tutor/{studentCourseDetails}"))
class EditTutorController extends BaseController {
	var profileService = Wire.auto[ProfileService]

	@ModelAttribute("editTutorCommand")
	def editTutorCommand(
			@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails,
			@RequestParam(value="currentTutor", required=false) currentTutor: Member,
			@RequestParam(value="remove", required=false) remove: Boolean,
			user: CurrentUser
			) =
		new EditTutorCommand(studentCourseDetails, Option(currentTutor), user, Option(remove).getOrElse(false))

	// initial form display
	@RequestMapping(value = Array("/edit","/add"),method=Array(GET))
	def editTutor(@ModelAttribute("editTutorCommand") cmd: EditTutorCommand, request: HttpServletRequest) = {
		Mav("tutor/edit/view",
			"studentCourseDetails" -> cmd.studentCourseDetails,
			"tutorToDisplay" -> cmd.currentTutor
		).noLayout()
	}


	@RequestMapping(value = Array("/edit", "/add"), method=Array(POST))
	def saveTutor(@ModelAttribute("editTutorCommand") cmd: EditTutorCommand, request: HttpServletRequest ) = {
		cmd.apply()

		Mav("tutor/edit/view",
			"student" -> cmd.studentCourseDetails.student,
			"tutorToDisplay" -> cmd.currentTutor
		)
	}
}
