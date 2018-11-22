package uk.ac.warwick.tabula.api.web.controllers.profiles

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.profiles.{ViewMemberRelationshipsCommand, ViewRelatedStudentsCommand}
import uk.ac.warwick.tabula.data.model.{Member, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringRelationshipServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/v1/member/{member}/relationships"))
class MemberRelationshipsController extends ApiController
	with GetMemberRelationshipsApi
	with AutowiringRelationshipServiceComponent

trait GetMemberRelationshipsApi {

	self: ApiController with RelationshipServiceComponent =>

	private def validRelationships(member: Member): Seq[StudentRelationshipType] = {
		relationshipService.allStudentRelationshipTypes.filter(r => securityService.can(user, Permissions.Profiles.StudentRelationship.Read(r), member))
	}

	@ModelAttribute("viewStudentRelationshipsCommand")
	def getCommand(@PathVariable member: Member): ViewMemberRelationshipsCommand.CommandType =
		 ViewMemberRelationshipsCommand(member)

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def getMember(@PathVariable member: Member, @ModelAttribute("viewStudentRelationshipsCommand") command: Appliable[ViewMemberRelationshipsCommand.Result]): Mav = {

		val relationshipsTypesWithStuDtls = command.apply().entities
		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok",
			"relationships" -> relationshipsTypesWithStuDtls.map { case (relationshipType, result) =>
				Map(
					"relationshipType" -> Map(
						"id" -> relationshipType.id,
						"agentRole" -> relationshipType.agentRole,
						"studentRole" -> relationshipType.studentRole
					),
					"students" -> result.map(scd => Map(
						"userId" -> scd.student.userId,
						"universityId" -> scd.student.universityId
					))
				)
			}
		)))
	}
}
