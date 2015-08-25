package uk.ac.warwick.tabula.commands.groups.admin

import org.joda.time.LocalTime
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{MapLocation, NamedLocation, Location, Module}
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.helpers.{FoundUser, SystemClockComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{SmallGroupServiceComponent, AutowiringSmallGroupServiceComponent, AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.services.timetables.{ScientiaHttpTimetableFetchingServiceComponent, ModuleTimetableFetchingServiceComponent, AutowiringScientiaConfigurationComponent, ScientiaHttpTimetableFetchingService}
import uk.ac.warwick.tabula.system.permissions.{RequiresPermissionsChecking, PermissionsCheckingMethods, PermissionsChecking}
import uk.ac.warwick.tabula.timetables.{TimetableEventType, TimetableEvent}
import scala.collection.JavaConverters._

import ImportSmallGroupEventsFromExternalSystemCommand._

object ImportSmallGroupEventsFromExternalSystemCommand {

	val RequiredPermission = Permissions.SmallGroups.Update

	def apply(module: Module, set: SmallGroupSet) =
		new ImportSmallGroupEventsFromExternalSystemCommandInternal(module, set)
			with ComposableCommand[SmallGroupSet]
			with ImportSmallGroupEventsFromExternalSystemPermissions
			with ImportSmallGroupEventsFromExternalSystemDescription
			with CommandSmallGroupEventGenerator
			with CommandSmallGroupEventUpdater
			with AutowiringSmallGroupServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringScientiaConfigurationComponent
			with SystemClockComponent
			with ScientiaHttpTimetableFetchingServiceComponent
			with LookupEventsFromModuleTimetable

	class EventToImport {
		def this(event: TimetableEvent) {
			this()
			timetableEvent = event
		}

		var timetableEvent: TimetableEvent = _
		var group: SmallGroup = _
		var overwrite: SmallGroupEvent = _
	}

}

trait ImportSmallGroupEventsFromExternalSystemRequestState {
	var eventsToImport: JList[EventToImport] = JArrayList()
}

trait ImportSmallGroupEventsFromExternalSystemCommandState {
	def module: Module
	def set: SmallGroupSet
}

class ImportSmallGroupEventsFromExternalSystemCommandInternal(val module: Module, val set: SmallGroupSet)
	extends CommandInternal[SmallGroupSet]
		with ImportSmallGroupEventsFromExternalSystemCommandState
		with ImportSmallGroupEventsFromExternalSystemRequestState {
	self: SmallGroupEventGenerator with SmallGroupEventUpdater with UserLookupComponent with SmallGroupServiceComponent =>

	override def applyInternal() = transactional() {
		eventsToImport.asScala
			.map { e => (e.timetableEvent, Option(e.group), Option(e.overwrite)) }
			.filter { case (_, group, _) => group.nonEmpty }
			.foreach { case (e, group, overwrite) =>
				val tutorUsercodes = e.staffUniversityIds.flatMap { id =>
					Option(userLookup.getUserByWarwickUniId(id)).collect { case FoundUser(u) => u }.map { _.getUserId }
				}

				overwrite match {
					case Some(event) =>
						// Overwrite the existing event
						updateEvent(module, set, group.get, event, e.weekRanges, e.day, e.startTime, e.endTime, e.location, tutorUsercodes)

					case _ =>
						// Create a new event
						createEvent(module, set, group.get, e.weekRanges, e.day, e.startTime, e.endTime, e.location, tutorUsercodes)
				}
			}

		smallGroupService.saveOrUpdate(set)
		set
	}
}

trait LookupEventsFromModuleTimetable {
	self: ImportSmallGroupEventsFromExternalSystemCommandState
		with ImportSmallGroupEventsFromExternalSystemRequestState
		with ModuleTimetableFetchingServiceComponent =>

	eventsToImport.clear()
	eventsToImport.addAll(timetableFetchingService.getTimetableForModule(module.code.toUpperCase).getOrElse(Nil)
		.filter { event => event.year == set.academicYear }
		.filter { event => event.eventType == TimetableEventType.Practical || event.eventType == TimetableEventType.Seminar }
		.sorted
		.map(new EventToImport(_))
		.asJava
	)
}

trait SmallGroupEventUpdater {
	def updateEvent(module: Module, set: SmallGroupSet, group: SmallGroup, event: SmallGroupEvent, weeks: Seq[WeekRange], day: DayOfWeek, startTime: LocalTime, endTime: LocalTime, location: Option[Location], tutorUsercodes: Seq[String]): SmallGroupEvent
}

trait CommandSmallGroupEventUpdater extends SmallGroupEventUpdater {
	def updateEvent(module: Module, set: SmallGroupSet, group: SmallGroup, event: SmallGroupEvent, weeks: Seq[WeekRange], day: DayOfWeek, startTime: LocalTime, endTime: LocalTime, location: Option[Location], tutorUsercodes: Seq[String]) = {
		val command = ModifySmallGroupEventCommand.edit(module, set, group, event)
		command.weekRanges = weeks
		command.day = day
		command.startTime = startTime
		command.endTime = endTime
		location.collect {
			case NamedLocation(name) => command.location = name

			case MapLocation(name, locationId) =>
				command.location = name
				command.locationId = locationId
		}

		// Remove existing tutors if we're overwriting. This means that if we update the tutor in Tabula and there isn't
		// one in S+, we don't overwrite it. If there is one in S+ and then it's overwritten to have no tutor any more,
		// then... sadness?
		if (tutorUsercodes.nonEmpty) command.tutors.clear()
		command.tutors.addAll(tutorUsercodes.asJavaCollection)

		command.apply()
	}
}

trait ImportSmallGroupEventsFromExternalSystemPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ImportSmallGroupEventsFromExternalSystemCommandState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		mustBeLinked(mandatory(set), mandatory(module))
		p.PermissionCheck(RequiredPermission, set)
	}

}

trait ImportSmallGroupEventsFromExternalSystemDescription extends Describable[SmallGroupSet] {
	self: ImportSmallGroupEventsFromExternalSystemCommandState =>

	override def describe(d: Description) =
		d.smallGroupSet(set)

}