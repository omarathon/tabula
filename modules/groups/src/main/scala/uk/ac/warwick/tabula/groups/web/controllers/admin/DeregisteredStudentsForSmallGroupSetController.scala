package uk.ac.warwick.tabula.groups.web.controllers.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.{PopulateOnForm, MemberOrUser, Appliable}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.groups.commands.admin.DeregisteredStudentsForSmallGroupSetCommand
import uk.ac.warwick.tabula.groups.commands.admin.DeregisteredStudentsForSmallGroupSetCommand.StudentNotInMembership
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._

@Controller
@RequestMapping(Array("/admin/module/{module}/groups/{smallGroupSet}/deregistered"))
class DeregisteredStudentsForSmallGroupSetController extends GroupsController with AutowiringProfileServiceComponent {

	type DeregisteredStudentsForSmallGroupSetCommand = Appliable[Seq[StudentNotInMembership]] with PopulateOnForm

	@ModelAttribute("command") def command(@PathVariable("module") module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet): DeregisteredStudentsForSmallGroupSetCommand =
		DeregisteredStudentsForSmallGroupSetCommand(module, set)

	@ModelAttribute("students") def students(@PathVariable("smallGroupSet") set: SmallGroupSet) =
		set.studentsNotInMembership.map { user =>
			val member = profileService.getMemberByUser(user, disableFilter = true)

			// Safe to do a .get here as studentsNotInMembership mandates that this is defined
			val group = set.groups.asScala.find { _.students.includesUser(user) }.get

			StudentNotInMembership(MemberOrUser(member, user), group)
		}

	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@PathVariable("smallGroupSet") set: SmallGroupSet,
		@ModelAttribute("command") cmd: DeregisteredStudentsForSmallGroupSetCommand
	) = {
		cmd.populate()
		renderForm(set, cmd)
	}

	private def renderForm(set: SmallGroupSet, cmd: DeregisteredStudentsForSmallGroupSetCommand) =
		Mav("admin/groups/deregistered/form")
			.crumbs(
				Breadcrumbs.DepartmentForYear(set.module.adminDepartment, set.academicYear),
				Breadcrumbs.ModuleForYear(set.module, set.academicYear)
			)

	@RequestMapping(method = Array(POST))
	def save(
		@Valid @ModelAttribute("command") cmd: DeregisteredStudentsForSmallGroupSetCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = {
		if (errors.hasErrors) renderForm(set, cmd)
		else {
			val removed = cmd.apply()

			Mav("admin/groups/deregistered/results",
				"removed" -> removed,
				"returnTo" -> Breadcrumbs.ModuleForYear(set.module, set.academicYear).url
			).crumbs(
				Breadcrumbs.DepartmentForYear(set.module.adminDepartment, set.academicYear),
				Breadcrumbs.ModuleForYear(set.module, set.academicYear)
			)
		}
	}

}
