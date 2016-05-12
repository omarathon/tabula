package uk.ac.warwick.tabula.web.controllers.profiles.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.{TaskBenchmarking, Appliable}
import uk.ac.warwick.tabula.data.model.{Department, StudentCourseDetails, StudentRelationshipType}
import uk.ac.warwick.tabula.commands.profiles.{ViewRelatedStudentsCommandState, MissingStudentRelationshipCommand, ViewRelatedStudentsCommand, ViewStudentRelationshipsCommand}
import uk.ac.warwick.tabula.services.AutowiringMeetingRecordServiceComponent
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
			"studentCount" -> agentGraph.studentCount,
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
class ViewStudentRelationshipStudentsController extends ProfilesController with AutowiringMeetingRecordServiceComponent with TaskBenchmarking {

	type ViewRelatedStudentsCommand = Appliable[Seq[StudentCourseDetails]] with ViewRelatedStudentsCommandState

	@ModelAttribute("viewRelatedStudentsCommand") def command(@PathVariable relationshipType: StudentRelationshipType) =
		ViewRelatedStudentsCommand(currentMember, relationshipType)

	@RequestMapping
	def view(@ModelAttribute("viewRelatedStudentsCommand") viewRelatedStudentsCommand: ViewRelatedStudentsCommand): Mav = {
		val results = viewRelatedStudentsCommand.apply()
		val students = results.map(_.student).distinct.sortBy { student =>  (student.lastName, student.firstName) }

		val meetingsMap = results.map(scd => {
			val rels = relationshipService.getRelationships(viewRelatedStudentsCommand.relationshipType, scd.student)
			val lastMeeting = benchmarkTask("lastMeeting"){
				meetingRecordService.list(rels.toSet, Some(currentMember)).headOption
			}
			scd.student.universityId -> lastMeeting
		}).toMap

		if(ajax)
			Mav("profiles/relationships/student_view_results",
				"studentCourseDetails" -> results,
				"students" -> students,
				"meetingsMap" -> meetingsMap
			).noLayout()
		else
			Mav("profiles/relationships/student_view",
				"studentCourseDetails" -> results,
				"students" -> students,
				"meetingsMap" -> meetingsMap
			)
	}
}
