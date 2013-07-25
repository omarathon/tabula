package uk.ac.warwick.tabula.groups.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{InitBinder, PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.tabula.data.model.Module
import org.springframework.web.bind.WebDataBinder
import uk.ac.warwick.tabula.commands.{UpstreamGroupPropertyEditor, UpstreamGroup}
import uk.ac.warwick.tabula.groups.commands.admin.EditGroupSetEnrolmentCommand

/**
 * Controller to populate the user listing for editing, without persistence
 */
@Controller
@RequestMapping(value = Array("/admin/module/{module}/groups/enrolment"))
class GroupsEnrolmentController extends GroupsController {

	validatesSelf[EditGroupSetEnrolmentCommand]

	@ModelAttribute def formObject(@PathVariable("module") module: Module) = {
		val cmd = new EditGroupSetEnrolmentCommand(mandatory(module))
		cmd.upstreamGroups.clear()
		cmd
	}

	@RequestMapping
	def showForm(form: EditGroupSetEnrolmentCommand, openDetails: Boolean = false) = {
		form.afterBind()

		Mav("admin/groups/enrolment",
			"department" -> form.module.department,
			"module" -> form.module,
			"availableUpstreamGroups" -> form.availableUpstreamGroups,
			"linkedUpstreamAssessmentGroups" -> form.linkedUpstreamAssessmentGroups,
			"assessmentGroups" -> form.assessmentGroups,
			"openDetails" -> openDetails)
			.noLayout()
	}

	@InitBinder
	def upstreamGroupBinder(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
	}

}
