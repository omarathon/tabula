package uk.ac.warwick.tabula.commands.admin.markingdescriptors

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.DepartmentMarkingDescriptor
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringMarkingDescriptorServiceComponent, MarkingDescriptorServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object DeleteMarkingDescriptorCommand {
	def apply(markingDescriptor: DepartmentMarkingDescriptor): Appliable[Unit] =
		new DeleteMarkingDescriptorCommandInternal(markingDescriptor)
			with ComposableCommand[Unit]
			with DeleteMarkingDescriptorState
			with DeleteMarkingDescriptorDescription
			with DeleteMarkingDescriptorPermissions
			with AutowiringMarkingDescriptorServiceComponent
}

class DeleteMarkingDescriptorCommandInternal(val markingDescriptor: DepartmentMarkingDescriptor) extends CommandInternal[Unit] {
	self: MarkingDescriptorServiceComponent with DeleteMarkingDescriptorState =>

	override protected def applyInternal(): Unit = {
		markingDescriptorService.delete(markingDescriptor)
	}
}

trait DeleteMarkingDescriptorState {
	val markingDescriptor: DepartmentMarkingDescriptor
}

trait DeleteMarkingDescriptorPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DeleteMarkingDescriptorState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.Department.Manage, mandatory(markingDescriptor.department))
	}
}

trait DeleteMarkingDescriptorDescription extends Describable[Unit] {
	self: DeleteMarkingDescriptorState =>

	override def describe(d: Description): Unit = {
		d.department(markingDescriptor.department).markingDescriptor(markingDescriptor)
	}
}
