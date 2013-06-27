package uk.ac.warwick.tabula.groups.web.views

import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.web.views.ViewModel._

/**
 * A selection of view model classes for passing to the template.
 *
 * See ViewModel for some common components.
 *
 * TODO building the Menu in code means deciding things like the text and icon
 * in Scala instead of Freemarker. Is this bad? Maybe just contain key decisions
 * like whether we are a manager, then the template can decide whether to render
 * those items.
 */
object GroupsViewModel {
	case class ViewModules(
		moduleItems: Seq[ViewModule],
		canManageDepartment: Boolean
	)
	case class ViewModule(
		module: Module,
		setItems: Seq[ViewSet],
		canManageGroups: Boolean
	)
	case class ViewSet(
		set: SmallGroupSet
	)
}
