package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.web.Mav

@Controller
class ViewProfileByCourseDetailsController extends ViewProfileController {

	@ModelAttribute("viewProfileCommandForStudentCourseDetails")
	def viewProfileCommandForStudentCourseDetails(@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails)
		=  {
			mandatory(studentCourseDetails).student match {
				case student: StudentMember => new ViewProfileCommand(user, student)
				case _ => throw new ItemNotFoundException
			}
	}

	// get the profile for the latest year
	@RequestMapping(Array("/view/course/{studentCourseDetails}"))
	def viewProfileForStudentCourseDetails(
		@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails,
		@ModelAttribute("viewProfileCommandForStudentCourseDetails") profileCmd: Appliable[StudentMember],
		@RequestParam(value = "meeting", required = false) openMeetingId: String,
		@RequestParam(defaultValue = "", required = false) agentId: String): Mav = {

			val profiledStudentMember = profileCmd.apply()
			viewProfileForCourse(Some(studentCourseDetails), Some(studentCourseDetails.latestStudentCourseYearDetails), openMeetingId, agentId, profiledStudentMember)
	}

	// get the profile for the chosen year
	@RequestMapping(Array("/view/course/{studentCourseDetails}/{year}"))
	def viewProfileForStudentCourseDetailsAndYear(
		@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails,
		@PathVariable("year") year: AcademicYear,
		@ModelAttribute("viewProfileCommandForStudentCourseDetails") profileCmd: Appliable[StudentMember],
		@RequestParam(value = "meeting", required = false) openMeetingId: String,
		@RequestParam(defaultValue = "", required = false) agentId: String): Mav = {

			val profiledStudentMember = profileCmd.apply()
			viewProfileForCourse(Some(studentCourseDetails), studentCourseYearFromYear(studentCourseDetails, year), openMeetingId, agentId, profiledStudentMember)
	}
}
