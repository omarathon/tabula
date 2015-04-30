package uk.ac.warwick.tabula.commands.timetables

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.helpers.SystemClockComponent
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.timetables.{AutowiringScientiaConfigurationComponent, ModuleTimetableFetchingService, ModuleTimetableFetchingServiceComponent, ScientiaHttpTimetableFetchingService}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.timetables.TimetableEvent

import scala.util.Try

object ViewModuleTimetableCommand {
	def apply(module: Module) =
		new ViewModuleTimetableCommandInternal(module)
			with ComposableCommand[Try[Seq[TimetableEvent]]]
			with ViewModuleTimetablePermissions
			with ViewModuleTimetableValidation
			with Unaudited with ReadOnly
			with AutowiringScientiaConfigurationComponent
			with SystemClockComponent
			with ModuleTimetableFetchingServiceComponent {
			// Only include Scientia events for now. If we ever include from other sources, they should be opt-in via params
			val timetableFetchingService = ScientiaHttpTimetableFetchingService(scientiaConfiguration)
		}

	// Re-usable service
	def apply(module: Module, service: ModuleTimetableFetchingService) =
		new ViewModuleTimetableCommandInternal(module)
			with ComposableCommand[Try[Seq[TimetableEvent]]]
			with ViewModuleTimetablePermissions
			with ViewModuleTimetableValidation
			with Unaudited with ReadOnly
			with ModuleTimetableFetchingServiceComponent {
			// Only include Scientia events for now. If we ever include from other sources, they should be opt-in via params
			val timetableFetchingService = service
		}
}

trait ViewModuleTimetableCommandFactory {
	def apply(module: Module): ComposableCommand[Try[Seq[TimetableEvent]]]
}
class ViewModuleTimetableCommandFactoryImpl(service: ModuleTimetableFetchingService) extends ViewModuleTimetableCommandFactory {
	def apply(module: Module) = ViewModuleTimetableCommand(module, service)
}

abstract class ViewModuleTimetableCommandInternal(val module: Module)
	extends CommandInternal[Try[Seq[TimetableEvent]]]
		with ViewModuleTimetableRequest {

	self: ModuleTimetableFetchingServiceComponent =>

	def applyInternal(): Try[Seq[TimetableEvent]] = {
		timetableFetchingService.getTimetableForModule(module.code.toUpperCase)
			.map { events => events.filter { event => event.year == academicYear }}
	}
}

// State - unmodifiable pre-requisites
trait ViewModuleTimetableState {
	val module: Module
}

// Request parameters
trait ViewModuleTimetableRequest extends ViewModuleTimetableState
	with CurrentSITSAcademicYear

trait ViewModuleTimetablePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewModuleTimetableState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Module.ViewTimetable, mandatory(module))
	}
}

trait ViewModuleTimetableValidation extends SelfValidating {
	self: ViewModuleTimetableRequest =>

	override def validate(errors: Errors) {
		if (academicYear == null) {
			errors.rejectValue("academicYear", "NotEmpty")
		}
	}
}