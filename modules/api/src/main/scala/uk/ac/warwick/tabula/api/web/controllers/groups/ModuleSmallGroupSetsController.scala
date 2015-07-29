package uk.ac.warwick.tabula.api.web.controllers.groups

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.api.web.helpers.{SmallGroupToJsonConverter, SmallGroupEventToJsonConverter, AssessmentMembershipInfoToJsonConverter, SmallGroupSetToJsonConverter}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.groups.commands.admin.{AdminSmallGroupsHomeCommand, AdminSmallGroupsHomeCommandState, AdminSmallGroupsHomeInformation}
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

@Controller
@RequestMapping(Array("/v1/module/{module}/groups"))
class ModuleSmallGroupSetsController extends ApiController
	with ListSmallGroupSetsForModuleApi
	with SmallGroupSetToJsonConverter
	with SmallGroupToJsonConverter
	with SmallGroupEventToJsonConverter
	with AssessmentMembershipInfoToJsonConverter

trait ListSmallGroupSetsForModuleApi {
	self: ApiController with SmallGroupSetToJsonConverter =>

	type AdminSmallGroupsHomeCommand = Appliable[AdminSmallGroupsHomeInformation] with AdminSmallGroupsHomeCommandState

	@ModelAttribute("listCommand")
	def command(@PathVariable module: Module, @RequestParam(required = false) academicYear: AcademicYear, user: CurrentUser): AdminSmallGroupsHomeCommand = {
		val year = Option(academicYear).getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))

		AdminSmallGroupsHomeCommand(mandatory(module).adminDepartment, year, user, calculateProgress = false)
	}

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def list(@ModelAttribute("listCommand") command: AdminSmallGroupsHomeCommand, errors: Errors, @PathVariable module: Module) = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val info = command.apply()

			Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok",
				"academicYear" -> command.academicYear.toString,
				"groups" -> info.setsWithPermission.filter { _.set.module == module }.map { viewSet => jsonSmallGroupSetObject(viewSet) }
			)))
		}
	}
}