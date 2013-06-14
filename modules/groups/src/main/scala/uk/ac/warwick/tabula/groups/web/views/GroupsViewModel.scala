package uk.ac.warwick.tabula.groups.web.views

import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet

/**
 * A selection of view model classes for passing to the template.
 *
 * See ViewModel for some common components.
 */
object GroupsViewModel {
	case class ViewModules(moduleItems: Seq[ViewModule])
	case class ViewModule(
		module: Module,
		setItems: Seq[ViewSet],
		canManage: Boolean
	)
	case class ViewSet(set: SmallGroupSet)
}
