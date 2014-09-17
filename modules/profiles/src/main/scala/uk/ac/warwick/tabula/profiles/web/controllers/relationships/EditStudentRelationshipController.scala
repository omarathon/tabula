package uk.ac.warwick.tabula.profiles.web.controllers.relationships

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{ItemNotFoundException, CurrentUser}
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.profiles.commands.relationships.EditStudentRelationshipCommand
import uk.ac.warwick.tabula.services.{MaintenanceModeService, RelationshipService, ProfileService}
import uk.ac.warwick.tabula.web.controllers.BaseController
import javax.validation.Valid
import org.springframework.validation.Errors

/**
 * This is the controller for the case where a single old student relationship is replaced with another
 * through the Student Profile screen.  So although the student may have two existing tutors, only one
 * will be replaced.
 */
@Controller
@RequestMapping(Array("/{relationshipType}/{studentCourseDetails}"))
class EditStudentRelationshipController extends BaseController {

	validatesSelf[EditStudentRelationshipCommand]

	var profileService = Wire.auto[ProfileService]
	var relationshipService = Wire.auto[RelationshipService]
	var maintenanceMode = Wire[MaintenanceModeService]

	@ModelAttribute("editStudentRelationshipCommand")
	def editStudentRelationshipCommand(
			@PathVariable("relationshipType") relationshipType: StudentRelationshipType,
			@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails,
			@RequestParam(value="currentAgent", required=false) currentAgent: Member,
			@RequestParam(value="remove", required=false) remove: Boolean,
			user: CurrentUser
			) = {
		val currentAgents: Seq[Member] = if (currentAgent != null) Seq(currentAgent) else Seq()
		val cmd = new EditStudentRelationshipCommand(studentCourseDetails, relationshipType, currentAgents, user, Option(remove).getOrElse(false))
		cmd
	}

	// initial form display
	@RequestMapping(value = Array("/edit","/add"),method=Array(GET))
	def editAgent(@ModelAttribute("editStudentRelationshipCommand") cmd: EditStudentRelationshipCommand, errors: Errors) = {
		Mav("relationships/edit/view",
			"studentCourseDetails" -> cmd.studentCourseDetails,
			"agentToDisplay" -> currentAgent(cmd)
		).noLayout()
	}

	@RequestMapping(value = Array("/edit", "/add"), method=Array(POST))
	def saveAgent(@Valid @ModelAttribute("editStudentRelationshipCommand") cmd: EditStudentRelationshipCommand, errors: Errors) = {
		if(errors.hasErrors){
			editAgent(cmd, errors)
		} else {
			cmd.apply()

			Mav("relationships/edit/view",
				"student" -> cmd.studentCourseDetails.student,
				"agentToDisplay" -> currentAgent(cmd)
			)
		}
	}

	def currentAgent(cmd: EditStudentRelationshipCommand): Option[Member] = cmd.currentAgents.headOption
}
