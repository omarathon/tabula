package uk.ac.warwick.tabula.coursework.web.controllers

import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.coursework.web.controllers.StudentCourseworkCommand.StudentAssignments

abstract class StudentCourseworkController extends CourseworkController {
	def getMav(member: Member, info: StudentAssignments) =
		Mav("home/_student",
			"student" -> member,
			"enrolledAssignments" -> info.enrolledAssignments,
			"historicAssignments" -> info.historicAssignments,
			"isSelf" -> (member.universityId == user.universityId),
			"ajax" -> ajax
		).noLayoutIf(ajax)
}
