package uk.ac.warwick.tabula.commands.groups.admin

import org.joda.time.LocalTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{MapLocation, NamedLocation, Location, Module}
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.helpers.{FoundUser, SystemClockComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.timetables.{AutowiringScientiaConfigurationComponent, ModuleTimetableFetchingServiceComponent, ScientiaHttpTimetableFetchingServiceComponent}
import uk.ac.warwick.tabula.services.{UserLookupComponent, SmallGroupServiceComponent, AutowiringUserLookupComponent, AutowiringSmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.timetables.TimetableEventType
import scala.collection.JavaConverters._

object UpdateSmallGroupEventFromExternalSystemCommand {

	def apply(module: Module, set: SmallGroupSet, group: SmallGroup, event: SmallGroupEvent) =
		new UpdateSmallGroupEventFromExternalSystemCommandInternal(module, set, group, event)
			with ComposableCommand[SmallGroupEvent]
			with UpdateSmallGroupEventFromExternalSystemPermissions
			with UpdateSmallGroupEventFromExternalSystemDescription
			with UpdateSmallGroupEventFromExternalSystemValidation
			with ModifySmallGroupEventScheduledNotifications
			with CommandSmallGroupEventUpdater
			with AutowiringSmallGroupServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringScientiaConfigurationComponent
			with SystemClockComponent
			with ScientiaHttpTimetableFetchingServiceComponent

}

trait UpdateSmallGroupEventFromExternalSystemRequestState {
	var index: JInteger = _
}

trait UpdateSmallGroupEventFromExternalSystemCommandState extends ImportSmallGroupEventsFromExternalSystemCommandState {
	self: UpdateSmallGroupEventFromExternalSystemRequestState
		with ModuleTimetableFetchingServiceComponent =>

	def group: SmallGroup
	def event: SmallGroupEvent

	lazy val timetableEvents = timetableFetchingService.getTimetableForModule(module.code.toUpperCase).getOrElse(Nil)
		.filter { event => event.year == set.academicYear }
		.filter { event => event.eventType == TimetableEventType.Practical || event.eventType == TimetableEventType.Seminar }
		.sorted

}

class UpdateSmallGroupEventFromExternalSystemCommandInternal(val module: Module, val set: SmallGroupSet, val group: SmallGroup, val event: SmallGroupEvent)
	extends CommandInternal[SmallGroupEvent]
		with UpdateSmallGroupEventFromExternalSystemCommandState
		with UpdateSmallGroupEventFromExternalSystemRequestState {
	self: SmallGroupServiceComponent
		with UserLookupComponent
		with ModuleTimetableFetchingServiceComponent
		with SmallGroupEventUpdater =>

	override def applyInternal(): SmallGroupEvent = transactional() {
		val e = timetableEvents(index)

		val tutorUsercodes = e.staffUniversityIds.flatMap { id =>
			Option(userLookup.getUserByWarwickUniId(id)).collect { case FoundUser(u) => u.getUserId }
		}

		updateEvent(module, set, group, event, e.weekRanges, e.day, e.startTime, e.endTime, e.location, tutorUsercodes)

		smallGroupService.saveOrUpdate(event)
		smallGroupService.getOrCreateSmallGroupEventOccurrences(event)
		event
	}

}

trait UpdateSmallGroupEventFromExternalSystemValidation extends SelfValidating {
	self: UpdateSmallGroupEventFromExternalSystemRequestState =>

	override def validate(errors: Errors): Unit = {
		if (index == null) {
			errors.rejectValue("index", "NotEmpty")
		}
	}
}

trait UpdateSmallGroupEventFromExternalSystemPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: UpdateSmallGroupEventFromExternalSystemCommandState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		mustBeLinked(mandatory(set), mandatory(module))
		mustBeLinked(mandatory(group), mandatory(set))
		mustBeLinked(mandatory(event), mandatory(group))
		p.PermissionCheck(Permissions.SmallGroups.Update, event)
	}

}

trait UpdateSmallGroupEventFromExternalSystemDescription extends Describable[SmallGroupEvent] {
	self: UpdateSmallGroupEventFromExternalSystemCommandState =>

	override def describe(d: Description) =
		d.smallGroupEvent(event)

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