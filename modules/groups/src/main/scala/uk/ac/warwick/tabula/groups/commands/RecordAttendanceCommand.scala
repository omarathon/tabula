package uk.ac.warwick.tabula.groups.commands

import scala.collection.JavaConverters._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.services.ProfileServiceComponent
import uk.ac.warwick.tabula.services.UserLookupComponent
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventOccurrence
import uk.ac.warwick.tabula.services.SmallGroupServiceComponent
import uk.ac.warwick.tabula.services.AutowiringSmallGroupServiceComponent
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventAttendance
import uk.ac.warwick.tabula.services.TermServiceComponent
import org.joda.time.DateTime
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import RecordAttendanceCommand._
import uk.ac.warwick.tabula.ItemNotFoundException
import scala.Some

object RecordAttendanceCommand {
	type UniversityId = String
	
	def apply(event: SmallGroupEvent, week: Int, user: CurrentUser) =
		new RecordAttendanceCommand(event, week, user)
			with ComposableCommand[(SmallGroupEventOccurrence, Seq[SmallGroupEventAttendance])]
			with SmallGroupEventInFutureCheck
			with RecordAttendanceCommandPermissions
			with RecordAttendanceDescription
			with RecordAttendanceCommandValidation
			with AutowiringSmallGroupServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringProfileServiceComponent
			with AutowiringTermServiceComponent {
		override lazy val eventName = "RecordAttendance"
	}
}

abstract class RecordAttendanceCommand(val event: SmallGroupEvent, val week: Int, val user: CurrentUser) 
	extends CommandInternal[(SmallGroupEventOccurrence, Seq[SmallGroupEventAttendance])] 
		with RecordAttendanceState 
		with PopulateOnForm
		with TaskBenchmarking {
	self: SmallGroupServiceComponent with UserLookupComponent with ProfileServiceComponent =>
		
	if (!event.group.groupSet.collectAttendance) throw new ItemNotFoundException
		
	lazy val occurrence = transactional() { smallGroupService.getOrCreateSmallGroupEventOccurrence(event, week) }
	
	var studentsState: JMap[UniversityId, AttendanceState] = 
		LazyMaps.create { member: UniversityId => null: AttendanceState }.asJava
	
	lazy val members: Seq[MemberOrUser] = {
		(event.group.students.users.map { user =>
			val member = profileService.getMemberByUniversityId(user.getWarwickId)
			MemberOrUser(member, user)
		} ++ occurrence.attendance.asScala.toSeq.map { a =>
			val member = profileService.getMemberByUniversityId(a.universityId)
			val user = userLookup.getUserByWarwickUniId(a.universityId)
			MemberOrUser(member, user)
		}).distinct.sortBy(mou => (mou.lastName, mou.firstName, mou.universityId))
	}

	lazy val attendanceNotes = benchmarkTask("Get attendance notes") {
		smallGroupService.findAttendanceNotes(members.map(_.universityId), Seq(occurrence)).groupBy(_.student).map{
			case (student, notes) => MemberOrUser(student) -> notes.groupBy(_.occurrence)
		}.toMap
	}
	
	def populate() {		
		studentsState = members.map { member =>
			member.universityId -> 
				occurrence.attendance.asScala
							.find { _.universityId == member.universityId }
							.flatMap { a => Option(a.state) }.getOrElse(null)
		}.toMap.asJava
	}

	def applyInternal() = {
		val occurrence = transactional() { smallGroupService.getOrCreateSmallGroupEventOccurrence(event, week) }
		
		val attendances = studentsState.asScala.flatMap { case (studentId, state) =>
			if (state == null) {
				smallGroupService.deleteAttendance(studentId, event, week)
				None
			} else {
				Some(smallGroupService.saveOrUpdateAttendance(studentId, event, week, state, user))
			}
		}.toSeq
		
		(occurrence, attendances)
	}
}

trait RecordAttendanceCommandValidation extends SelfValidating {
	self: RecordAttendanceState with UserLookupComponent with SmallGroupEventInFutureCheck =>
	
	def validate(errors: Errors) {
		val invalidUsers: Seq[UniversityId] = studentsState.asScala.map { case (studentId, _) => studentId }.filter(s => !userLookup.getUserByWarwickUniId(s).isFoundUser).toSeq
		if (invalidUsers.length > 0) {
			errors.rejectValue("studentsState", "smallGroup.attendees.invalid", Array(invalidUsers), "")
		}
		
		// TAB-1791 Allow attendance to be recorded for users not in the group, they were in the group in the past or submitting would be a pain
		/*else {
			val missingUsers: Seq[UniversityId] = studentsState.asScala.map { case (studentId, _) => studentId }.filter(s => event.group.students.users.filter(u => u.getWarwickId() == s).length == 0).toSeq
			if (missingUsers.length > 0) {
				errors.rejectValue("studentsState", "smallGroup.attendees.missing", Array(missingUsers), "")
			}
		}*/
		
		studentsState.asScala.foreach { case (studentId, state) => 
			errors.pushNestedPath(s"studentsState[$studentId]")
			
			if (isFutureEvent && !(state == null || state == AttendanceState.MissedAuthorised)) {
				errors.rejectValue("", "smallGroup.attendance.beforeEvent")
			}
			
			errors.popNestedPath()
		}
	}
	
}

trait RecordAttendanceCommandPermissions extends RequiresPermissionsChecking {
	self: RecordAttendanceState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroupEvents.Register, event)
	}
}

trait RecordAttendanceState {
	val event: SmallGroupEvent
	val week: Int
	
	def studentsState: JMap[UniversityId, AttendanceState]
	def members: Seq[MemberOrUser]
}

trait SmallGroupEventInFutureCheck {
	self: RecordAttendanceState with TermServiceComponent =>
	
	lazy val isFutureEvent = {
		val academicYear = event.group.groupSet.academicYear
		val currentAcademicWeek = termService.getAcademicWeekForAcademicYear(DateTime.now, academicYear)
		
		currentAcademicWeek < week
	}
}

trait RecordAttendanceDescription extends Describable[(SmallGroupEventOccurrence, Seq[SmallGroupEventAttendance])] {
	this: RecordAttendanceState =>
	def describe(d: Description) {
		d.smallGroupEvent(event)
		d.property("week", week)
	}
}
