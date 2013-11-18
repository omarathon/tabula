package uk.ac.warwick.tabula.groups.commands

import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventOccurrence
import uk.ac.warwick.tabula.services.SmallGroupServiceComponent
import uk.ac.warwick.tabula.services.AutowiringSmallGroupServiceComponent
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import scala.collection.immutable.SortedMap
import uk.ac.warwick.tabula.services.TermServiceComponent
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import org.joda.time.DateTime

sealed abstract class SmallGroupAttendanceState {
	def getName = toString()
}

object SmallGroupAttendanceState {
	case object Attended extends SmallGroupAttendanceState
	case object Missed extends SmallGroupAttendanceState
	case object NotRecorded extends SmallGroupAttendanceState
	case object Late extends SmallGroupAttendanceState
}

object ViewSmallGroupAttendanceCommand {
	type EventInstance = (SmallGroupEvent, SmallGroupEventOccurrence.WeekNumber)
	type PerUserAttendance = SortedMap[User, SortedMap[EventInstance, SmallGroupAttendanceState]]
	
	case class SmallGroupAttendanceInformation(
		instances: Seq[EventInstance],
		attendance: PerUserAttendance
	)
	
	def apply(group: SmallGroup) =
		new ViewSmallGroupAttendanceCommand(group)
			with ComposableCommand[SmallGroupAttendanceInformation]
			with ViewSmallGroupAttendancePermissions
			with AutowiringSmallGroupServiceComponent
			with AutowiringTermServiceComponent
			with ReadOnly with Unaudited {
		override lazy val eventName = "ViewSmallGroupAttendance"
	}
		
	// Sort users by last name, first name
	implicit val defaultOrderingForUser = Ordering.by { user: User => (user.getLastName, user.getFirstName, user.getUserId) }
	
	implicit val defaultOrderingForEventInstance = Ordering.by { instance: EventInstance => instance match {
		case (event, week) => 
			val weekValue = week * 7 * 24
			val dayValue = (event.day.getAsInt - 1) * 24
			val hourValue = event.startTime.getHourOfDay
			
			(weekValue + dayValue + hourValue, week, event.id)
	}}
	
	def allEventInstances(group: SmallGroup, occurrences: Seq[SmallGroupEventOccurrence]) =
		group.events.asScala.filter { !_.isUnscheduled }.flatMap { event =>
			val allWeeks = event.weekRanges.flatMap { _.toWeeks }
			allWeeks.map { week => 
				val occurrence = occurrences.find { o =>
					o.event == event && o.week == week
				}
				
				((event, week), occurrence)
			}
		}
		
	def attendanceForStudent(allEventInstances: Seq[(EventInstance, Option[SmallGroupEventOccurrence])], isLate: EventInstance => Boolean)(user: User) = {
		val userAttendance = allEventInstances.map { case ((event, week), occurrence) =>
			val instance = (event, week)
			val state = occurrence match {
				case Some(occurrence) if occurrence.attendees.includesUser(user) => SmallGroupAttendanceState.Attended
				case Some(occurrence) => SmallGroupAttendanceState.Missed
				case None if isLate(event, week) => SmallGroupAttendanceState.Late
				case _ => SmallGroupAttendanceState.NotRecorded
			}
			
			(instance -> state)
		} 
		
		SortedMap(userAttendance.toSeq:_*)
	}
}

class ViewSmallGroupAttendanceCommand(val group: SmallGroup) 
	extends CommandInternal[ViewSmallGroupAttendanceCommand.SmallGroupAttendanceInformation] with ViewSmallGroupAttendanceState with TaskBenchmarking {
	self: SmallGroupServiceComponent with TermServiceComponent =>
		
	import ViewSmallGroupAttendanceCommand._
	
	override def applyInternal() = {
		val occurrences = benchmarkTask("Get all small group event occurrences for the group") { smallGroupService.findAttendanceByGroup(group) }
			
		// Build a list of all the events and week information, with an optional register
		val instances = benchmarkTask("Translate small group events into instances") { allEventInstances(group, occurrences) }
		
		// Build the list of all users who are in the group, or have attended one or more occurrences of the group
		val allStudents = benchmarkTask("Get a list of all registered or attended users") {
			group.students.users ++
			occurrences.flatMap { _.attendees.users }
			.distinct
		}
		
		val attendance = benchmarkTask("For each student, build an attended list for each instance") { 
			val attendance = allStudents.map { user => (user -> attendanceForStudent(instances, isLate)(user)) }
			
			SortedMap(attendance.toSeq:_*)
		}
		
		SmallGroupAttendanceInformation(
			instances = instances.map { case ((event, week), occurrence) => (event, week) }.sorted,
			attendance = attendance
		)
	}
	
	lazy val currentAcademicWeek = termService.getAcademicWeekForAcademicYear(DateTime.now, group.groupSet.academicYear)
	
	private def isLate(instance: EventInstance): Boolean = instance match {
		case (_, week: SmallGroupEventOccurrence.WeekNumber) =>
			week < currentAcademicWeek // only late if week is in the past
	}
	
}

trait ViewSmallGroupAttendancePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewSmallGroupAttendanceState =>
	
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroupEvents.ViewRegister, group)
	}
}

trait ViewSmallGroupAttendanceState {
	def group: SmallGroup
}