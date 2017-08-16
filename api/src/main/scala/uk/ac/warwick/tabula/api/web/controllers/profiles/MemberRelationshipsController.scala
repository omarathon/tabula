package uk.ac.warwick.tabula.api.web.controllers.profiles

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands.profiles.ViewRelatedStudentsCommand
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

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def getMember(@PathVariable member: Member): Mav = {
		val allRelationships = relationshipService.allStudentRelationshipTypes.map(relationshipType =>
			relationshipType -> ViewRelatedStudentsCommand(mandatory(member), relationshipType).apply()
		).filter { case (_, result) => result.entities.nonEmpty }
		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok",
			"relationships" -> allRelationships.map { case (relationshipType, result) =>
				Map(
					"relationshipType" -> Map(
						"id" -> relationshipType.id,
						"agentRole" -> relationshipType.agentRole,
						"studentRole" -> relationshipType.studentRole
					),
					"students" -> result.entities.map(scd => Map(
						"userId" -> scd.student.userId,
						"universityId" -> scd.student.universityId
					))
				)
			}
		)))
	}
}
