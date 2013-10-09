package uk.ac.warwick.tabula.services

import scala.collection.JavaConverters._
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.{AutowiringSmallGroupDaoComponent, SmallGroupDaoComponent, Daoisms}
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.AssignmentMembershipDao
import uk.ac.warwick.tabula.data.AssignmentMembershipDaoComponent
import uk.ac.warwick.tabula.data.AutowiringAssignmentMembershipDaoComponent
import uk.ac.warwick.tabula.data.model.ModuleRegistration
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.UserGroupDaoComponent
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.data.AutowiringUserGroupDaoComponent

trait SmallGroupServiceComponent {
	def smallGroupService: SmallGroupService
}

trait AutowiringSmallGroupServiceComponent extends SmallGroupServiceComponent {
	var smallGroupService = Wire[SmallGroupService]
}

trait SmallGroupService {
	def getSmallGroupSetById(id: String): Option[SmallGroupSet]
	def getSmallGroupById(id: String): Option[SmallGroup]
	def getSmallGroupEventById(id: String): Option[SmallGroupEvent]
	def getSmallGroupEventOccurrenceById(id: String): Option[SmallGroupEventOccurrence]
	def saveOrUpdate(smallGroupSet: SmallGroupSet)
	def saveOrUpdate(smallGroup: SmallGroup)
	def saveOrUpdate(smallGroupEvent: SmallGroupEvent)
	def findSmallGroupEventsByTutor(user: User): Seq[SmallGroupEvent]
	def findSmallGroupsByTutor(user: User): Seq[SmallGroup]
	def removeFromSmallGroups(moduleRegistration: ModuleRegistration)

	def findSmallGroupsByStudent(student: User): Seq[SmallGroup]
	def findSmallGroupSetsByMember(user:User):Seq[SmallGroupSet]

	def updateAttendance(smallGroupEvent: SmallGroupEvent, weekNumber: Int, universityIds: Seq[String]): SmallGroupEventOccurrence
	def getAttendees(event: SmallGroupEvent, weekNumber: Int): JList[String]
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

	def saveOrUpdate(smallGroupSet: SmallGroupSet) = smallGroupDao.saveOrUpdate(smallGroupSet)
	def saveOrUpdate(smallGroup: SmallGroup) = smallGroupDao.saveOrUpdate(smallGroup)
	def saveOrUpdate(smallGroupEvent: SmallGroupEvent) = smallGroupDao.saveOrUpdate(smallGroupEvent)

	def findSmallGroupEventsByTutor(user: User): Seq[SmallGroupEvent] = eventTutorsHelper.findBy(user)
	def findSmallGroupsByTutor(user: User): Seq[SmallGroup] = groupTutorsHelper.findBy(user)

	def findSmallGroupSetsByMember(user:User):Seq[SmallGroupSet] = membershipDao.getEnrolledSmallGroupSets(user)
	def findSmallGroupsByStudent(user: User): Seq[SmallGroup] = studentGroupHelper.findBy(user)

	def getAttendees(event: SmallGroupEvent, weekNumber: Int): JList[String] =
		smallGroupDao.getSmallGroupEventOccurrence(event, weekNumber) match {
			case Some(occurrence) => occurrence.attendees.includeUsers
			case _ => JArrayList()
		}

	def updateAttendance(event: SmallGroupEvent, weekNumber: Int, universityIds: Seq[String]): SmallGroupEventOccurrence = {
		val occurrence = smallGroupDao.getSmallGroupEventOccurrence(event, weekNumber) getOrElse {
			val newOccurrence = new SmallGroupEventOccurrence()
			newOccurrence.event = event
			newOccurrence.week = weekNumber
			smallGroupDao.saveOrUpdate(newOccurrence)
			newOccurrence
		}

		occurrence.attendees.includeUsers.clear()
		occurrence.attendees.includeUsers.addAll(universityIds.asJava)
		occurrence
	}

	def removeFromSmallGroups(modReg: ModuleRegistration) {
		val userId = modReg.studentCourseDetails.student.userId
		val user = userLookup.getUserByUserId(userId)

		val groups = smallGroupDao.findByModuleAndYear(modReg.module, modReg.academicYear)
		for (smallGroup <- groups) {
			val userGroup = smallGroup.students

			userGroup match {
				case uGroup: UserGroup => {
					val userGroupForSet = smallGroup.groupSet.members
					userGroupForSet match {
						case uGroupForSet: UserGroup => {
							if (!uGroupForSet.includesUser(user) && modReg.module.department.autoGroupDeregistration) {
								// if the person is not in includeUsers for the small group set,
								// their membership is linked to SITS imports so we can delete them
								uGroup.remove(user)
								userGroupDao.saveOrUpdate(uGroup)
							}
						}
						case _ => logger.warn("Could not remove user from group - userGroup " + userGroupForSet + " was not of type UserGroup as expected.")
					}
				}
				case _ => logger.warn("Could not remove user from group - userGroup " + userGroup + " was not of type UserGroup as expected.")
			}
		}
	}
}

trait SmallGroupMembershipHelpers {
	val eventTutorsHelper: UserGroupMembershipHelper[SmallGroupEvent]
  //TODO can this be removed? findSmallGroupsByTutor could just call findSmallGroupEventsByTutor and then group by group
	val groupTutorsHelper: UserGroupMembershipHelper[SmallGroup]
	val studentGroupHelper: UserGroupMembershipHelper[SmallGroup]
	val membershipDao: AssignmentMembershipDao
}

// new up UGMHs which will Wire.auto() their dependencies
trait SmallGroupMembershipHelpersImpl extends SmallGroupMembershipHelpers {
	val eventTutorsHelper = new UserGroupMembershipHelper[SmallGroupEvent]("tutors")
	val groupTutorsHelper = new UserGroupMembershipHelper[SmallGroup]("events.tutors")

	// Don't use this, it's misleading - it won't use linked assessment components
//	val groupSetMembersHelper = new UserGroupMembershipHelper[SmallGroupSet]("_membersGroup")

	val studentGroupHelper = new UserGroupMembershipHelper[SmallGroup]("_studentsGroup")
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
