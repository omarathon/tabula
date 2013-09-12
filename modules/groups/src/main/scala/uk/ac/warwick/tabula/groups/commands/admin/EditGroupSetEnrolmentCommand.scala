package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.permissions.{Permissions, CheckablePermission}


/**
 * This is a stub class, which isn't applied, but exposes the student membership (enrolment) for an assignment
 * via the ModifyAssignmentCommand to rebuild views within an existing form
 */
class EditGroupSetEnrolmentCommand(module: Module = null) extends ModifySmallGroupSetCommand(module) with Unaudited {


	PermissionCheckAny(
		Seq(CheckablePermission(Permissions.SmallGroups.Create, module),
			CheckablePermission(Permissions.SmallGroups.Update, module))
	)

	// not required
	override def applyInternal() = throw new UnsupportedOperationException

	// not required
	val setOption = None
}
