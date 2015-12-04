package uk.ac.warwick.tabula.commands.timetables

import org.joda.time.{Interval, LocalDate}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.helpers.SystemClockComponent
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.timetables.EventOccurrence

import scala.concurrent.Await
import scala.util.Try
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global

object ViewModuleEventsCommand {
	val Timeout = 15.seconds

	def apply(module: Module) =
		new ViewModuleEventsCommandInternal(module)
			with ComposableCommand[Try[Seq[EventOccurrence]]]
			with ViewModuleEventsPermissions
			with ViewModuleEventsValidation
			with Unaudited with ReadOnly
			with AutowiringScientiaConfigurationComponent
			with SystemClockComponent
			with ModuleTimetableFetchingServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringTermBasedEventOccurrenceServiceComponent {
			// Only include Scientia events for now. If we ever include from other sources, they should be opt-in via params
			val timetableFetchingService = ScientiaHttpTimetableFetchingService(scientiaConfiguration)
		}

	// Re-usable service
	def apply(module: Module, service: ModuleTimetableFetchingService) =
		new ViewModuleEventsCommandInternal(module)
			with ComposableCommand[Try[Seq[EventOccurrence]]]
			with ViewModuleEventsPermissions
			with ViewModuleEventsValidation
			with Unaudited with ReadOnly
			with ModuleTimetableFetchingServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringTermBasedEventOccurrenceServiceComponent {
			val timetableFetchingService = service
		}
}

trait ViewModuleEventsCommandFactory {
	def apply(module: Module): ComposableCommand[Try[Seq[EventOccurrence]]]
}
class ViewModuleEventsCommandFactoryImpl(service: ModuleTimetableFetchingService) extends ViewModuleEventsCommandFactory {
	def apply(module: Module) = ViewModuleEventsCommand(module, service)
}

abstract class ViewModuleEventsCommandInternal(val module: Module)
	extends CommandInternal[Try[Seq[EventOccurrence]]]
		with ViewModuleEventsRequest {

	self: ModuleTimetableFetchingServiceComponent with TermServiceComponent with EventOccurrenceServiceComponent =>

	def applyInternal(): Try[Seq[EventOccurrence]] = {
		val timetableEvents = timetableFetchingService.getTimetableForModule(module.code.toUpperCase)
			.map { events =>
				if (academicYear != null) {
					events.filter { event => event.year == academicYear }
				} else {
					events
				}
			}

		if (start == null) {
			start = termService.getTermFromDate(academicYear.dateInTermOne).getStartDate.toLocalDate
		}

		if (end == null) {
			end = termService.getTermFromDate((academicYear + 1).dateInTermOne).getStartDate.toLocalDate.minusDays(1)
		}

		Try(Await.result(timetableEvents.map { events =>
			// Converter to make localDates sortable
			import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

			events.flatMap { event =>
				eventOccurrenceService.fromTimetableEvent(event, new Interval(start.toDateTimeAtStartOfDay, end.toDateTimeAtStartOfDay))
			}.sortBy(_.start)
		}, ViewModuleEventsCommand.Timeout))
	}
}

// State - unmodifiable pre-requisites
trait ViewModuleEventsState {
	val module: Module
}

// Request parameters
trait ViewModuleEventsRequest extends ViewModuleEventsState {
	var academicYear: AcademicYear = null
	var start: LocalDate = LocalDate.now.minusMonths(12)
	var end: LocalDate = start.plusMonths(13)
}

trait ViewModuleEventsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewModuleEventsState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Module.ViewTimetable, mandatory(module))
	}
}

trait ViewModuleEventsValidation extends SelfValidating {
	self: ViewModuleEventsRequest =>

	override def validate(errors: Errors) {
		// Must have either an academic year or a start and an end
		if (academicYear == null && (start == null || end == null)) {
			errors.rejectValue("academicYear", "NotEmpty")
		}
	}
}