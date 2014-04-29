package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.coursework.commands.assignments.AssignMarkersCommand
import uk.ac.warwick.tabula.data.model.{UserGroup, Assignment, Module}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.services.{AssignmentMembershipService, UserLookupService}
import org.springframework.beans.factory.annotation.Autowired

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/assign-markers"))
class AssignMarkersController extends CourseworkController {

	case class Marker(fullName: String, userCode: String, students: Seq[Student])
	case class Student(displayValue: String, userCode: String)

	@Autowired var userLookup: UserLookupService = _

	@Autowired var assignmentMembershipService: AssignmentMembershipService = _

	@ModelAttribute("command")
	def getCommand(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		AssignMarkersCommand(module, assignment)

	@ModelAttribute("firstMarkerRoleName")
	def firstMarkerRoleName(@PathVariable assignment: Assignment): String = assignment.markingWorkflow.firstMarkerRoleName

	@ModelAttribute("secondMarkerRoleName")
	def secondMarkerRoleName(@PathVariable assignment: Assignment): Option[String] = assignment.markingWorkflow.secondMarkerRoleName

	@ModelAttribute("firstMarkers")
	def firstMarkers(@PathVariable module: Module, @PathVariable assignment: Assignment): Seq[Marker] =
		retrieveMarkers(module, assignment, assignment.markingWorkflow.firstMarkers.members, assignment.firstMarkerMap)

	@ModelAttribute("secondMarkers")
	def secondMarkers(@PathVariable module: Module, @PathVariable assignment: Assignment): Seq[Marker] =
		assignment.markingWorkflow.hasSecondMarker match {
			case true => retrieveMarkers(module, assignment, assignment.markingWorkflow.secondMarkers.members, assignment.secondMarkerMap)
			case false => Seq()
		}

	private def retrieveMarkers(module: Module, assignment: Assignment, markers: Seq[String], markerMap: Map[String, UserGroup]): Seq[Marker] = {
		markers.map { markerId =>
			val assignedStudents: Seq[Student] = markerMap.get(markerId) match {
				case Some(usergroup: UserGroup) => usergroup.users.map{ student =>
					val displayValue = module.department.showStudentName match {
						case true => student.getFullName
						case false => student.getWarwickId
					}
					Student(displayValue, student.getUserId)
				}
				case None => Seq()
			}

			val user = Option(userLookup.getUserByUserId(markerId))
			val fullName = user match{
				case Some(u) => u.getFullName
				case None => ""
			}

			Marker(fullName, markerId, assignedStudents)
		}
	}

	@RequestMapping()
	def form(
		@ModelAttribute("command") cmd: Appliable[Assignment],
		@ModelAttribute("firstMarkers") firstMarkers: Seq[Marker],
		@ModelAttribute("secondMarkers") secondMarkers: Seq[Marker],
		@PathVariable module: Module,
		@PathVariable assignment: Assignment
	) = {
		val members = assignmentMembershipService.determineMembershipUsers(assignment).map{s =>
			val displayValue = module.department.showStudentName match {
				case true => s.getFullName
				case false => s.getWarwickId
			}
			new Student(displayValue, s.getUserId)
		}

		val firstMarkerUnassignedStudents = members.toList filterNot firstMarkers.map(_.students).flatten.contains
		val secondMarkerUnassignedStudents = members.toList filterNot secondMarkers.map(_.students).flatten.contains

		Mav("admin/assignments/assignmarkers/form",
			"hasSecondMarker" -> assignment.markingWorkflow.hasSecondMarker,
			"firstMarkerUnassignedStudents" -> firstMarkerUnassignedStudents,
			"secondMarkerUnassignedStudents" -> secondMarkerUnassignedStudents
		).crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}

	@RequestMapping(method = Array(POST))
	def doUpload(
		@PathVariable module: Module,
		@PathVariable(value = "assignment") assignment: Assignment,
		@ModelAttribute("command") cmd: Appliable[Assignment]
	) = {
		cmd.apply()
		Redirect(Routes.admin.module(module))
	}

}
