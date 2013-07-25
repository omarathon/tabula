package uk.ac.warwick.tabula.groups.web.controllers

import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.groups.web.GroupsBreadcrumbs
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.{ViewModules, ViewSet, ViewModule}

abstract class GroupsController extends BaseController with GroupsBreadcrumbs {

	hideDeletedItems

	final override def onPreRequest {

	}

	type moduleGroupsMap = Map[Module, Map[SmallGroupSet, Seq[SmallGroup]]]

	def generateGroupsViewModel(mapping: moduleGroupsMap): ViewModules = {
		val moduleItems = for ((module, sets) <- mapping) yield {
			ViewModule(module,
				sets.toSeq map { case (set, groups) =>
					ViewSet(set, groups)
				},
				canManageGroups = false
			)
		}

		ViewModules( moduleItems.toSeq, canManageDepartment = false )	}
	
}