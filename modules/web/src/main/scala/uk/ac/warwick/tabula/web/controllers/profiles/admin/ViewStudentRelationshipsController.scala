package uk.ac.warwick.tabula.web.controllers.profiles.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.{Department, StudentRelationshipType}
import uk.ac.warwick.tabula.commands.profiles.{MissingStudentRelationshipCommand, ViewRelatedStudentsCommand, ViewStudentRelationshipsCommand}
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(value = Array("/profiles/department/{department}/{relationshipType}"))
class ViewStudentRelationshipsController extends ProfilesController {

	@ModelAttribute("viewStudentRelationshipsCommand")
	def viewStudentRelationshipsCommand(
		@PathVariable department: Department,
		@PathVariable relationshipType: StudentRelationshipType
	) = new ViewStudentRelationshipsCommand(department, relationshipType)

	@RequestMapping(method = Array(HEAD, GET))
	def view(
		@PathVariable department: Department,
		@ModelAttribute("viewStudentRelationshipsCommand") command: ViewStudentRelationshipsCommand
	): Mav = {
		val agentGraph = command.apply()
		Mav("profiles/relationships/agent_view",
			"agentRelationships" -> agentGraph.studentMap,
			"missingCount" -> agentGraph.missingCount,
			"courseMap" -> agentGraph.courseMap,
			"yearOfStudyMap" -> agentGraph.yearOfStudyMap,
			"department" -> department
		)
	}
}

@Controller
@RequestMapping(value = Array("/profiles/department/{department}/{relationshipType}/missing"))
class MissingStudentRelationshipController extends ProfilesController {

	@ModelAttribute("missingStudentRelationshipCommand")
	def missingStudentRelationshipCommand(
		@PathVariable department: Department,
		@PathVariable relationshipType: StudentRelationshipType
	) =	new MissingStudentRelationshipCommand(department, relationshipType)

	@RequestMapping(method = Array(HEAD, GET))
	def view(
		@PathVariable department: Department,
		@ModelAttribute("missingStudentRelationshipCommand") missing: MissingStudentRelationshipCommand
	): Mav = {
		val (studentCount, missingStudents) = missing.apply()
		Mav("profiles/relationships/missing_agent_view",
			"studentCount" -> studentCount,
			"missingStudents" -> missingStudents,
			"department" -> department
		)
	}
}

@Controller
@RequestMapping(value = Array("/profiles/{relationshipType}/students"))
class ViewStudentRelationshipStudentsController extends ProfilesController {

	type ViewRelatedStudentsCommand = ViewRelatedStudentsCommand.CommandType

	@ModelAttribute("viewRelatedStudentsCommand") def command(@PathVariable relationshipType: StudentRelationshipType) =
		ViewRelatedStudentsCommand(currentMember, relationshipType)

	@RequestMapping
	def view(@ModelAttribute("viewRelatedStudentsCommand") viewRelatedStudentsCommand: ViewRelatedStudentsCommand): Mav = {
		val result = viewRelatedStudentsCommand.apply()
		val studentCourseDetails = result.entities
		val students = studentCourseDetails.map(_.student).distinct.sortBy { student =>  (student.lastName, student.firstName) }
		val meetingInfoMap = result.lastMeetingWithTotalPendingApprovalsMap

		if(ajax)
			Mav("profiles/relationships/student_view_results",
				"studentCourseDetails" -> studentCourseDetails,
				"students" -> students,
				"meetingsMap" -> meetingInfoMap
			).noLayout()
		else
			Mav("profiles/relationships/student_view",
				"studentCourseDetails" -> studentCourseDetails,
				"students" -> students,
				"meetingsMap" -> meetingInfoMap
			)
	}
}
