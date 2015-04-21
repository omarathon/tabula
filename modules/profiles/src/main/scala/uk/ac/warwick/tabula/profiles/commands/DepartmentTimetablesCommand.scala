package uk.ac.warwick.tabula.profiles.commands

import org.joda.time.{DateTime, Interval, LocalDate}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.timetables.ViewModuleTimetableCommandFactory
import uk.ac.warwick.tabula.data.model.{Module, Department}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.timetables.{AutowiringTermBasedEventOccurrenceServiceComponent, EventOccurrenceServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEvent}
import collection.JavaConverters._
import scala.util.{Failure, Success}

object DepartmentTimetablesCommand {
	def apply(department: Department, moduleTimetableCommandFactory: ViewModuleTimetableCommandFactory) =
		new DepartmentTimetablesCommandInternal(department, moduleTimetableCommandFactory)
			with ComposableCommand[Seq[EventOccurrence]]
			with ReadOnly with Unaudited
			with DepartmentTimetablesPermissions
			with DepartmentTimetablesCommandState
			with DepartmentTimetablesCommandRequest
			with AutowiringTermBasedEventOccurrenceServiceComponent
}


class DepartmentTimetablesCommandInternal(val department: Department, val moduleTimetableCommandFactory: ViewModuleTimetableCommandFactory)
	extends CommandInternal[Seq[EventOccurrence]] {

	self: DepartmentTimetablesCommandRequest with EventOccurrenceServiceComponent =>

	override def applyInternal() = {
		val moduleCommands = modules.asScala.map(moduleTimetableCommandFactory.apply)
		val moduleEvents = moduleCommands.flatMap(cmd => cmd.apply() match {
			case Success(events) =>
				events
			case Failure(t) =>
				Seq()
		}).distinct

		val allEvents = moduleEvents

		val occurrences = allEvents.flatMap(eventsToOccurrences)

		// Converter to make localDates sortable
		import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
		occurrences.sortBy(_.start)
	}

	private def eventsToOccurrences: TimetableEvent => Seq[EventOccurrence] =
		eventOccurrenceService.fromTimetableEvent(_, new Interval(start.toDateTimeAtStartOfDay, end.toDateTimeAtStartOfDay))

}

trait DepartmentTimetablesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: DepartmentTimetablesCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.Search, department)
	}

}

trait DepartmentTimetablesCommandState {
	def department: Department
}

trait DepartmentTimetablesCommandRequest extends PermissionsCheckingMethods {

	self: DepartmentTimetablesCommandState =>

	var modules: JList[Module] = JArrayList()
	var from: JLong = LocalDate.now.minusMonths(1).toDateTimeAtStartOfDay.getMillis
	var to: JLong = LocalDate.now.plusMonths(1).toDateTimeAtStartOfDay.getMillis
	def start = new DateTime(from * 1000).toLocalDate
	def end = new DateTime(to * 1000).toLocalDate

	private def modulesForDepartmentAndSubDepartments(department: Department): Seq[Module] =
		(department.modules.asScala ++ department.children.asScala.flatMap { modulesForDepartmentAndSubDepartments }).sorted

	lazy val allModules: Seq[Module] = ((modulesForDepartmentAndSubDepartments(mandatory(department)) match {
		case Nil => modulesForDepartmentAndSubDepartments(mandatory(department.rootDepartment))
		case someModules => someModules
	}) ++ modules.asScala).distinct.sorted
}
