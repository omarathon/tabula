#if ((${PACKAGE_NAME} && ${PACKAGE_NAME} != ""))package ${PACKAGE_NAME} #end
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.Errors
object COMMANDNAMECommand {
	def apply =
		new COMMANDNAMECommandInternal
		with ComposableCommand[RETURNTYPE]
		with COMMANDNAMEValidation
		with COMMANDNAMEDescription
		with COMMANDNAMEPermissions
		with COMMANDNAMECommandState
}
class COMMANDNAMECommandInternal extends CommandInternal[RETURNTYPE] {
	override def applyInternal() = {

	}
}
trait COMMANDNAMEValidation extends SelfValidating {

	self: COMMANDNAMECommandState =>
	override def validate(errors: Errors) {

	}
}
trait COMMANDNAMEPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: COMMANDNAMECommandState =>
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck()
	}
}
trait COMMANDNAMEDescription extends Describable[RETURNTYPE] {

	self: COMMANDNAMECommandState =>
	override lazy val eventName = "COMMANDNAME"
	override def describe(d: Description) {
	}
}
trait COMMANDNAMECommandState {
}
trait COMMANDNAMECommandRequest {
}
