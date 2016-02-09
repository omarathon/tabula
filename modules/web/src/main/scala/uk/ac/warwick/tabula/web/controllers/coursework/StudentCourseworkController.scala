package uk.ac.warwick.tabula.web.controllers.coursework

import uk.ac.warwick.tabula.commands.coursework.assignments.StudentCourseworkCommand.StudentAssignments
import uk.ac.warwick.tabula.data.model.{StudentCourseYearDetails, Member}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, MemberOrUser}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.commands.coursework.assignments.{StudentCourseworkGadgetCommand, StudentCourseworkFullScreenCommand}

abstract class StudentCourseworkController extends CourseworkController {
	def getMav(member: Member, info: StudentAssignments) =
		Mav("coursework/home/_student",
			"student" -> member,
			"enrolledAssignments" -> info.enrolledAssignments,
			"historicAssignments" -> info.historicAssignments,
			"isSelf" -> (member.universityId == user.universityId),
			"ajax" -> ajax
		).noLayoutIf(ajax)
}

@Controller
@RequestMapping(Array("/coursework/student/{member}"))
class StudentCourseworkFullScreenController extends StudentCourseworkController {

	@ModelAttribute("command") def command(@PathVariable member: Member) =
		StudentCourseworkFullScreenCommand(MemberOrUser(member))

	@RequestMapping
	def listAssignments(@ModelAttribute("command") command: Appliable[StudentAssignments], @PathVariable member: Member, user: CurrentUser): Mav =
		getMav(member, command.apply())

}

@Controller
@RequestMapping(Array("/coursework/student/bycourseandyear/{studentCourseYearDetails}"))
class StudentCourseworkGadgetController extends StudentCourseworkController {

	@ModelAttribute("command") def command(@PathVariable studentCourseYearDetails: StudentCourseYearDetails) =
		StudentCourseworkGadgetCommand(mandatory(studentCourseYearDetails))

	@RequestMapping
	def listAssignments(
		@ModelAttribute("command") command: Appliable[StudentAssignments],
		@PathVariable studentCourseYearDetails: StudentCourseYearDetails,
		user: CurrentUser
	): Mav =
		getMav(studentCourseYearDetails.studentCourseDetails.student, command.apply())

}