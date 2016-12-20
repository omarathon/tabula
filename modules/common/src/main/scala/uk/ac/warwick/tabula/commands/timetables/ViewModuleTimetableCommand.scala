package uk.ac.warwick.tabula.commands.timetables

import java.util.concurrent.TimeoutException

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.timetables.ViewModuleTimetableCommand.{CommandType, ReturnType}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.helpers.SystemClockComponent
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.concurrent.Await
import scala.util.Try

object ViewModuleTimetableCommand {
	private[timetables] type ReturnType = Try[EventList]
	type CommandType = Appliable[ReturnType] with ViewModuleTimetableRequest

	def apply(module: Module): CommandType =
		new ViewModuleTimetableCommandInternal(module)
			with ComposableCommand[ReturnType]
			with ViewModuleTimetablePermissions
			with ViewModuleTimetableValidation
			with ViewModuleTimetableDescription with ReadOnly
			with AutowiringScientiaConfigurationComponent
			with AutowiringNewScientiaConfigurationComponent
			with SystemClockComponent
			with ScientiaHttpTimetableFetchingServiceComponent // Only include Scientia events for now. If we ever include from other sources, they should be opt-in via params

	// Re-usable service
	def apply(module: Module, service: ModuleTimetableFetchingService): CommandType =
		new ViewModuleTimetableCommandInternal(module)
			with ComposableCommand[ReturnType]
			with ViewModuleTimetablePermissions
			with ViewModuleTimetableValidation
			with Unaudited with ReadOnly
			with ModuleTimetableFetchingServiceComponent {
			val timetableFetchingService: ModuleTimetableFetchingService = service
		}
}

trait ViewModuleTimetableCommandFactory {
	def apply(module: Module): CommandType
}
class ViewModuleTimetableCommandFactoryImpl(service: ModuleTimetableFetchingService) extends ViewModuleTimetableCommandFactory {
	def apply(module: Module) = ViewModuleTimetableCommand(module, service)
}

abstract class ViewModuleTimetableCommandInternal(val module: Module)
	extends CommandInternal[ReturnType]
		with ViewModuleTimetableRequest {

	self: ModuleTimetableFetchingServiceComponent =>

	def applyInternal(): ReturnType = {
		Try(Await.result(timetableFetchingService.getTimetableForModule(module.code.toUpperCase, includeStudents = false), ViewModuleEventsCommand.Timeout))
			.recover { case _: TimeoutException | _: TimetableEmptyException => EventList.empty }
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

/**
	* This won't be audited, but it is included in things like stopwatch task names
	*/
trait ViewModuleTimetableDescription extends Describable[ReturnType] with Unaudited {
	self: ViewModuleTimetableRequest =>

	override def describe(d: Description): Unit = d.module(module).properties("academicYear" -> academicYear.toString)
}