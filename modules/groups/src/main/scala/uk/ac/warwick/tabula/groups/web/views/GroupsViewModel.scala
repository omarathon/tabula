package uk.ac.warwick.tabula.groups.web.views

import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod.StudentSignUp
import uk.ac.warwick.tabula.web.views.ViewModel._
import scala.collection.JavaConverters._

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
	) {
		def hasUnreleasedGroupsets = moduleItems.exists(_.hasUnreleasedGroupsets)
		def hasOpenableGroupsets = moduleItems.exists(_.hasOpenableGroupsets)
	}

	case class ViewModule(
		module: Module,
		setItems: Seq[ViewSet],
		canManageGroups: Boolean
	) {
		def hasUnreleasedGroupsets = module.hasUnreleasedGroupSets
		def hasOpenableGroupsets = module.groupSets.asScala.exists(s => (!s.openForSignups) && s.allocationMethod == StudentSignUp )
	}

	case class ViewSet(
		set: SmallGroupSet,
		groups: Seq[SmallGroup]
	)

}
