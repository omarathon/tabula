package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.{AssignmentMembershipDao, AssignmentMembershipDaoComponent, AutowiringAssignmentMembershipDaoComponent, AutowiringSmallGroupDaoComponent, AutowiringUserGroupDaoComponent, SmallGroupDaoComponent, UserGroupDaoComponent}
import uk.ac.warwick.tabula.data.model.{Module, StudentMember, ModuleRegistration, UserGroup, UnspecifiedTypeUserGroup}
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.commands.groups.RemoveUserFromSmallGroupCommand
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import org.joda.time.DateTime

trait SmallGroupServiceComponent {
	def smallGroupService: SmallGroupService
}

trait AutowiringSmallGroupServiceComponent extends SmallGroupServiceComponent {
	var smallGroupService = Wire[SmallGroupService]
	var assignmentMembershipService = Wire[AssignmentMembershipService]
}

trait SmallGroupService {
	def getSmallGroupSetById(id: String): Option[SmallGroupSet]
	def getSmallGroupById(id: String): Option[SmallGroup]
	def getSmallGroupEventById(id: String): Option[SmallGroupEvent]
	def getSmallGroupEventOccurrenceById(id: String): Option[SmallGroupEventOccurrence]
	def getSmallGroupEventOccurrence(event: SmallGroupEvent, weekNumber: Int): Option[SmallGroupEventOccurrence]
	def getDepartmentSmallGroupSetById(id: String): Option[DepartmentSmallGroupSet]
	def getDepartmentSmallGroupById(id: String): Option[DepartmentSmallGroup]
	def getOrCreateSmallGroupEventOccurrence(event: SmallGroupEvent, weekNumber: Int): SmallGroupEventOccurrence
	def saveOrUpdate(smallGroupSet: SmallGroupSet)
	def saveOrUpdate(smallGroup: SmallGroup)
	def saveOrUpdate(smallGroupEvent: SmallGroupEvent)
	def saveOrUpdate(note: SmallGroupEventAttendanceNote)
	def saveOrUpdate(smallGroupSet: DepartmentSmallGroupSet)
	def saveOrUpdate(smallGroup: DepartmentSmallGroup)
	def findSmallGroupEventsByTutor(user: User): Seq[SmallGroupEvent]
	def findSmallGroupsByTutor(user: User): Seq[SmallGroup]
	def removeFromSmallGroups(moduleRegistration: ModuleRegistration)

	def findSmallGroupsByStudent(student: User): Seq[SmallGroup]
	def findSmallGroupSetsByMember(user:User):Seq[SmallGroupSet]

	def saveOrUpdateAttendance(studentId: String, event: SmallGroupEvent, weekNumber: Int, state: AttendanceState, user: CurrentUser): SmallGroupEventAttendance
	def deleteAttendance(studentId: String, event: SmallGroupEvent, weekNumber: Int): Unit
	def findAttendanceByGroup(smallGroup: SmallGroup): Seq[SmallGroupEventOccurrence]
	def getAttendanceNote(studentId: String, occurrence: SmallGroupEventOccurrence): Option[SmallGroupEventAttendanceNote]
	def findAttendanceNotes(studentIds: Seq[String], occurrences: Seq[SmallGroupEventOccurrence]): Seq[SmallGroupEventAttendanceNote]
	def getAttendance(studentId: String, occurrence: SmallGroupEventOccurrence) : Option[SmallGroupEventAttendance]

	def findAttendanceForStudentInModulesInWeeks(student: StudentMember, startWeek: Int, endWeek: Int, modules: Seq[Module]): Seq[SmallGroupEventAttendance]
	def hasSmallGroups(module: Module): Boolean
}

abstract class AbstractSmallGroupService extends SmallGroupService {
	self: SmallGroupDaoComponent
		with AssignmentMembershipDaoComponent
		with SmallGroupMembershipHelpers
		with UserLookupComponent
		with UserGroupDaoComponent
		with Logging =>

	def getSmallGroupSetById(id: String) = smallGroupDao.getSmallGroupSetById(id)
	def getSmallGroupById(id: String) = smallGroupDao.getSmallGroupById(id)
	def getSmallGroupEventById(id: String) = smallGroupDao.getSmallGroupEventById(id)
	def getSmallGroupEventOccurrenceById(id: String) = smallGroupDao.getSmallGroupEventOccurrenceById(id)
	def getSmallGroupEventOccurrence(event: SmallGroupEvent, weekNumber: Int) = smallGroupDao.getSmallGroupEventOccurrence(event, weekNumber)
	def getDepartmentSmallGroupSetById(id: String) = smallGroupDao.getDepartmentSmallGroupSetById(id)
	def getDepartmentSmallGroupById(id: String) = smallGroupDao.getDepartmentSmallGroupById(id)
	def getOrCreateSmallGroupEventOccurrence(event: SmallGroupEvent, weekNumber: Int) = 
		smallGroupDao.getSmallGroupEventOccurrence(event, weekNumber).getOrElse {
			val newOccurrence = new SmallGroupEventOccurrence()
			newOccurrence.event = event
			newOccurrence.week = weekNumber
			smallGroupDao.saveOrUpdate(newOccurrence)
			newOccurrence
		}

	def saveOrUpdate(smallGroupSet: SmallGroupSet) = smallGroupDao.saveOrUpdate(smallGroupSet)
	def saveOrUpdate(smallGroup: SmallGroup) = smallGroupDao.saveOrUpdate(smallGroup)
	def saveOrUpdate(smallGroupEvent: SmallGroupEvent) = smallGroupDao.saveOrUpdate(smallGroupEvent)
	def saveOrUpdate(note: SmallGroupEventAttendanceNote) = smallGroupDao.saveOrUpdate(note)
	def saveOrUpdate(smallGroupSet: DepartmentSmallGroupSet) = smallGroupDao.saveOrUpdate(smallGroupSet)
	def saveOrUpdate(smallGroup: DepartmentSmallGroup) = smallGroupDao.saveOrUpdate(smallGroup)

	def findSmallGroupEventsByTutor(user: User): Seq[SmallGroupEvent] = eventTutorsHelper.findBy(user)
	def findSmallGroupsByTutor(user: User): Seq[SmallGroup] = findSmallGroupEventsByTutor(user).groupBy(_.group).keys.toSeq

	def findSmallGroupSetsByMember(user:User):Seq[SmallGroupSet] = {
		val autoEnrolled = 
			membershipDao.getSITSEnrolledSmallGroupSets(user)
				 .filterNot { _.members.excludesUser(user) }

		val manuallyEnrolled = 
			groupSetManualMembersHelper.findBy(user)
				.filterNot { sgs => sgs.deleted || sgs.archived }
		
		(autoEnrolled ++ manuallyEnrolled).distinct
	}
	def findSmallGroupsByStudent(user: User): Seq[SmallGroup] = studentGroupHelper.findBy(user)

	def deleteAttendance(studentId: String, event: SmallGroupEvent, weekNumber: Int) {
		for {
			occurrence <- smallGroupDao.getSmallGroupEventOccurrence(event, weekNumber)
			attendance <- smallGroupDao.getAttendance(studentId, occurrence)
		} {
			smallGroupDao.deleteAttendance(attendance)
		}
	}
	
	def saveOrUpdateAttendance(
		studentId: String,
		event: SmallGroupEvent,
		weekNumber: Int,
		state: AttendanceState,
		user: CurrentUser
	): SmallGroupEventAttendance = {
		val occurrence = getOrCreateSmallGroupEventOccurrence(event, weekNumber)
		
		val attendance = smallGroupDao.getAttendance(studentId, occurrence).getOrElse({
			val newAttendance = new SmallGroupEventAttendance
			newAttendance.occurrence = occurrence
			newAttendance.universityId = studentId
			newAttendance
		})
		
		attendance.state = state
		attendance.updatedBy = user.userId
		attendance.updatedDate = DateTime.now
		smallGroupDao.saveOrUpdate(attendance)
		attendance
	}
	
	def findAttendanceByGroup(smallGroup: SmallGroup): Seq[SmallGroupEventOccurrence] = 
		smallGroupDao.findSmallGroupOccurrencesByGroup(smallGroup)

	def removeFromSmallGroups(modReg: ModuleRegistration) {
		if (modReg.module.department.autoGroupDeregistration) {
			val userId = modReg.studentCourseDetails.student.userId
			val user = userLookup.getUserByUserId(userId)

			for {
			    smallGroup <- smallGroupDao.findByModuleAndYear(modReg.module, modReg.academicYear)
			    if smallGroup.students.includesUser(user)
			} {
				// Wrap this in a sub-command so that we can do auditing
				userGroupDao.saveOrUpdate(removeFromGroupCommand(user, smallGroup).apply() match {
					case group: UserGroupCacheManager => group.underlying.asInstanceOf[UserGroup]
					case group: UnspecifiedTypeUserGroup => group.asInstanceOf[UserGroup]
				})
			}
		}
	}
	
	private def removeFromGroupCommand(user: User, smallGroup: SmallGroup): Appliable[UnspecifiedTypeUserGroup] = {
		new RemoveUserFromSmallGroupCommand(user, smallGroup)
	}

	def getAttendanceNote(studentId: String, occurrence: SmallGroupEventOccurrence): Option[SmallGroupEventAttendanceNote] =
		smallGroupDao.getAttendanceNote(studentId, occurrence)

	def findAttendanceNotes(studentIds: Seq[String], occurrences: Seq[SmallGroupEventOccurrence]): Seq[SmallGroupEventAttendanceNote] =
		smallGroupDao.findAttendanceNotes(studentIds, occurrences)

	def getAttendance(studentId: String, occurrence: SmallGroupEventOccurrence) : Option[SmallGroupEventAttendance] =
		smallGroupDao.getAttendance(studentId, occurrence)

	def findAttendanceForStudentInModulesInWeeks(student: StudentMember, startWeek: Int, endWeek: Int, modules: Seq[Module]) =
		smallGroupDao.findAttendanceForStudentInModulesInWeeks(student, startWeek, endWeek, modules)

	def hasSmallGroups(module: Module): Boolean = smallGroupDao.hasSmallGroups(module)
}

trait SmallGroupMembershipHelpers {
	val eventTutorsHelper: UserGroupMembershipHelper[SmallGroupEvent]
	val studentGroupHelper: UserGroupMembershipHelper[SmallGroup]
	val groupSetManualMembersHelper: UserGroupMembershipHelper[SmallGroupSet]
	val departmentStudentGroupHelper: UserGroupMembershipHelper[DepartmentSmallGroup]
	val departmentGroupSetManualMembersHelper: UserGroupMembershipHelper[DepartmentSmallGroupSet]
	val membershipDao: AssignmentMembershipDao
}

// new up UGMHs which will Wire.auto() their dependencies
trait SmallGroupMembershipHelpersImpl extends SmallGroupMembershipHelpers {
	val eventTutorsHelper = new UserGroupMembershipHelper[SmallGroupEvent]("_tutors")

	// This won't use linked assessment components, only manual membership
	val groupSetManualMembersHelper = new UserGroupMembershipHelper[SmallGroupSet]("_membersGroup")

	val studentGroupHelper = new UserGroupMembershipHelper[SmallGroup]("_studentsGroup")

	// This won't use linked assessment components, only manual membership
	val departmentGroupSetManualMembersHelper = new UserGroupMembershipHelper[DepartmentSmallGroupSet]("_membersGroup")

	val departmentStudentGroupHelper = new UserGroupMembershipHelper[DepartmentSmallGroup]("_studentsGroup")
}

@Service("smallGroupService")
class SmallGroupServiceImpl
	extends AbstractSmallGroupService
		with AutowiringSmallGroupDaoComponent
		with AutowiringAssignmentMembershipDaoComponent
	  with SmallGroupMembershipHelpersImpl
	  with AutowiringUserLookupComponent
		with UserLookupComponent
		with AutowiringUserGroupDaoComponent
		with Logging
