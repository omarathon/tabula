package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.profiles.commands.ViewModuleRegistrationsCommand

@Controller
@RequestMapping(Array("/view/modules/{studentCourseDetails}/{academicYear}"))
class ViewModuleRegistrationsController
	extends ProfilesController {

	@RequestMapping
	def home(
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable academicYear: AcademicYear) = {
		val scyd = studentCourseYearFromYear(studentCourseDetails, academicYear)
		val moduleRegs = ViewModuleRegistrationsCommand (
		  mandatory(scyd)
		).apply()

		Mav("profile/module_list",
		 "studentCourseYearDetails" -> scyd,
		  "moduleRegs" -> moduleRegs
		).noLayoutIf(ajax)
	}

	def studentCourseYearFromYear(studentCourseDetails: StudentCourseDetails, year: AcademicYear) =
		studentCourseDetails.freshStudentCourseYearDetails.filter(_.academicYear == year).seq.headOption

}
