package uk.ac.warwick.tabula.groups.web.controllers

import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.groups.web.GroupsBreadcrumbs
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.{ViewerRole, ViewModules, ViewSet, ViewModule}

abstract class GroupsController extends BaseController with GroupsBreadcrumbs {

	hideDeletedItems

	final override def onPreRequest {

	}

	type ModuleGroupsMap = Map[Module, Map[SmallGroupSet, Seq[SmallGroup]]]

	def generateViewModules(mapping: ModuleGroupsMap, viewerRole:ViewerRole): ViewModules = {
		val moduleItems = for ((module, sets) <- mapping) yield {
			ViewModule(module,
				sets.toSeq map { case (set, groups) =>
					ViewSet(set, groups, viewerRole)
				},
				canManageGroups = false
			)
		}

		ViewModules( moduleItems.toSeq.sortBy(_.module.code), canManageDepartment = false )
	}
	
}