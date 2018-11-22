package uk.ac.warwick.tabula.api.web.controllers.profiles

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.profiles.{ViewMemberRelationshipsCommand}
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.services.{AutowiringRelationshipServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/v1/member/{member}/relationships"))
class MemberRelationshipsController extends ApiController
	with GetMemberRelationshipsApi
	with AutowiringRelationshipServiceComponent

trait GetMemberRelationshipsApi {

	self: ApiController with RelationshipServiceComponent =>

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
