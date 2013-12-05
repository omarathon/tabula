package uk.ac.warwick.tabula.groups.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.groups.commands.ListGroupUnallocatedStudentsCommand
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(value=Array("/{smallGroupSet}/unallocatedstudentspopup"))
class ListGroupUnallocatedStudentsController extends GroupsController {

	@ModelAttribute("command")
	def command(@PathVariable smallGroupSet: SmallGroupSet) =
		new ListGroupUnallocatedStudentsCommand(smallGroupSet)

	@RequestMapping
	def ajaxList(@ModelAttribute("command") command: ListGroupUnallocatedStudentsCommand, user: CurrentUser): Mav = {

		val students = command.apply()
		val userIsMember = students.exists(_.universityId == user.universityId)
		val showTutors = command.smallGroupSet.studentsCanSeeTutorName

		Mav("groups/students",
			"students" -> students,
			"userUniId" -> user.universityId,
			"userIsMember" -> userIsMember,
			"studentsCanSeeTutorName" -> showTutors
		).noLayout()
	}

}
