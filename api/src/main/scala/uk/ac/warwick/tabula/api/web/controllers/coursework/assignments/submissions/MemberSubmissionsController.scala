package uk.ac.warwick.tabula.api.web.controllers.coursework.assignments.submissions

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestMethod}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.{SubmissionInfoToJsonConverter, SubmissionToJsonConverter}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.assignments.{ListSubmissionsResult, SubmissionsCommand}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}

abstract class MemberSubmissionsController extends ApiController
	with SubmissionToJsonConverter with SubmissionInfoToJsonConverter

object MemberSubmissionsController {
	type ViewMemberSubmissionsCommand = Appliable[ListSubmissionsResult]
}

@Controller
@RequestMapping(
	method = Array(RequestMethod.GET),
	value = Array("/v1/department/{department}/submissions/member/{member}")
)
class GetMemberSubmissionsController extends MemberSubmissionsController
	with GetMemberSubmissionsApi

trait GetMemberSubmissionsApi {
	self: MemberSubmissionsController =>

	@ModelAttribute("listCommand")
	def listCommand(@PathVariable department: Department, @PathVariable member: Member): Appliable[ListSubmissionsResult] =
		SubmissionsCommand(department, member)

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def list(@Valid @ModelAttribute("listCommand") command: Appliable[ListSubmissionsResult], errors: Errors): Mav = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val results = command.apply()

			Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok",
				"fromDate" -> results.fromDate,
				"toDate" -> results.toDate,
				"submissions" -> results.submissions.map(jsonSubmissionInfoObject)

			)))
		}
	}
}