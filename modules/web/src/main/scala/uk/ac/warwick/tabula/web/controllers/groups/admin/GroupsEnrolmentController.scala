package uk.ac.warwick.tabula.web.controllers.groups.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{InitBinder, PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.commands.groups.admin.{ModifiesSmallGroupSetMembership, EditSmallGroupSetMembershipCommand}
import uk.ac.warwick.tabula.data.model.Module
import org.springframework.web.bind.WebDataBinder
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating, UpstreamGroupPropertyEditor, UpstreamGroup}
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController

/**
* Controller to populate the user listing for editing, without persistence
*/
@Controller
@RequestMapping(value = Array("/groups/admin/module/{module}/groups/{smallGroupSet}/enrolment"))
class GroupsEnrolmentController extends GroupsController {

	validatesSelf[SelfValidating]
	type EditSmallGroupSetMembershipCommand = Appliable[SmallGroupSet] with ModifiesSmallGroupSetMembership

	@ModelAttribute("command") def formObject(@PathVariable("module") module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet) = {
		val cmd = EditSmallGroupSetMembershipCommand.stub(mandatory(module), mandatory(set))
		cmd.upstreamGroups.clear()
		cmd
	}

	@RequestMapping
	def showForm(@Valid @ModelAttribute("command") form: EditSmallGroupSetMembershipCommand, openDetails: Boolean = false) = {
		form.afterBind()

		Mav("groups/admin/groups/enrolment",
			"department" -> form.module.adminDepartment,
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
