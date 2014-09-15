package uk.ac.warwick.tabula.groups.web.controllers.admin

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.commands.{PopulateOnForm, SelfValidating, Appliable}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.groups.commands.admin.{ImportSmallGroupSetsFromExternalSystemPermissionsRestrictedState, LookupEventsFromModuleTimetables, ImportSmallGroupSetsFromExternalSystemCommandState, ImportSmallGroupSetsFromExternalSystemCommand}
import uk.ac.warwick.tabula.web.Routes

@Controller
@RequestMapping(value = Array("/admin/department/{department}/import-groups"))
class ImportSmallGroupSetsFromExternalSystemController extends GroupsController {

	validatesSelf[SelfValidating]

	type ImportSmallGroupSetsFromExternalSystemCommand =
		Appliable[Seq[SmallGroupSet]]
			with ImportSmallGroupSetsFromExternalSystemCommandState
			with ImportSmallGroupSetsFromExternalSystemPermissionsRestrictedState
			with LookupEventsFromModuleTimetables
			with PopulateOnForm

	@ModelAttribute("academicYearChoices") def academicYearChoices =
		AcademicYear.guessSITSAcademicYearByDate(DateTime.now).yearsSurrounding(2, 2)

	@ModelAttribute("command") def command(@PathVariable department: Department, user: CurrentUser): ImportSmallGroupSetsFromExternalSystemCommand =
		ImportSmallGroupSetsFromExternalSystemCommand(mandatory(department), mandatory(user))

	// The initial load of page 1, where we select the items to import.
	@RequestMapping
	def showForm(@ModelAttribute("command") command: ImportSmallGroupSetsFromExternalSystemCommand, errors: Errors) = {
		command.populate()
		form(command, errors)
	}

	private def form(command: ImportSmallGroupSetsFromExternalSystemCommand, errors: Errors) =
		Mav("admin/groups/import",
			"academicYear" -> command.academicYear,
			"modules" -> command.modules,
			"timetabledEvents" -> command.timetabledEvents
		).crumbs(Breadcrumbs.Department(command.department))

	// Change the academic year; restarts from scratch
	@RequestMapping(method = Array(POST), params = Array("action=change-year"))
	def changeYear(@ModelAttribute("command") command: ImportSmallGroupSetsFromExternalSystemCommand, errors: Errors) =
		showForm(command, errors) // Run an initial populate() again

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") command: ImportSmallGroupSetsFromExternalSystemCommand, errors: Errors) =
		if (errors.hasErrors) form(command, errors)
		else {
			command.apply()
			Redirect(Routes.groups.admin(command.department, command.academicYear))
		}

}
