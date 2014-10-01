package uk.ac.warwick.tabula.groups.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Appliable, ViewViewableCommand}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.groups.services.AutowiringSmallGroupSetWorkflowServiceComponent
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.{SetProgress, Tutor, ViewSetWithProgress}
import uk.ac.warwick.tabula.permissions.Permissions
import scala.collection.JavaConverters._

@Controller
@RequestMapping(value=Array("/admin/module/{module}/groups/{smallGroupSet}"))
class AdminSmallGroupSetController extends GroupsController with AutowiringSmallGroupSetWorkflowServiceComponent {

	hideDeletedItems

	@ModelAttribute("adminCommand") def command(@PathVariable("module") module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet) = {
		mustBeLinked(mandatory(set), mandatory(module))
		new ViewViewableCommand(Permissions.Module.ManageSmallGroups, set)
	}

	@RequestMapping
	def adminSingleSet(@ModelAttribute("adminCommand") cmd: Appliable[SmallGroupSet], user: CurrentUser) = {
		val set = cmd.apply()

		val progress = smallGroupSetWorkflowService.progress(set)

		val setView = ViewSetWithProgress(
			set = set,
			groups = set.groups.asScala.sorted,
			viewerRole = Tutor,
			progress = SetProgress(progress.percentage, progress.cssClass, progress.messageCode),
			nextStage = progress.nextStage,
			stages = progress.stages
		)

		val model = Map(
			"set" -> setView
		)

		if (ajax) Mav("admin/module/single_set-noLayout", model).noLayout()
		else Mav("admin/module/single_set", model).crumbs(Breadcrumbs.DepartmentForYear(set.module.department, set.academicYear), Breadcrumbs.Module(set.module))
	}

}
