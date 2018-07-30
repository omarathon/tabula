package uk.ac.warwick.tabula.commands.scheduling

import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Describable, Description}
import uk.ac.warwick.tabula.data.{AutowiringModuleRegistrationDaoComponent, ModuleRegistrationDao, ModuleRegistrationDaoComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsService, PermissionsServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object RemoveAgedModuleRegistrationCommand {
	def apply() =
		new RemoveAgedModuleRegistrationCommandInternal
			with ComposableCommand[Seq[String]]
			with AutowiringPermissionsServiceComponent
			with AutowiringModuleRegistrationDaoComponent
			with RemoveAgedModuleRegistrationCommandPermission
			with RemoveAgedModuleRegistrationCommandDescription
}

class RemoveAgedModuleRegistrationCommandInternal
	extends CommandInternal[Seq[String]] with Logging {

	self: PermissionsServiceComponent
		with ModuleRegistrationDaoComponent =>
	override protected def applyInternal(): Seq[String] = (	for {ids <- Some(moduleRegistrationDao.getOrphaned)}
		yield {
			moduleRegistrationDao.deleteByIds(ids)
			ids
		}).getOrElse(Seq.empty)
}

trait RemoveAgedModuleRegistrationCommandPermission
	extends RequiresPermissionsChecking
		with PermissionsCheckingMethods {
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}
}

trait RemoveAgedModuleRegistrationCommandDescription extends Describable[Seq[String]] {
	override def describe(d: Description) {}

	override def describeResult(d: Description, result: Seq[String]): Unit = {
		d.properties(("ModRegIds", result))
	}
}