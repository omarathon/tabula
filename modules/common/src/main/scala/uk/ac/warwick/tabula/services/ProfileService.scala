package uk.ac.warwick.tabula.services

import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.PrsCode
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import scala.collection.JavaConverters._
import scala.Some

/**
 * Service providing access to members and profiles.
 */
trait ProfileService {
	def save(member: Member)
	def getRegisteredModules(universityId: String): Seq[Module]
	def getMemberByUniversityId(universityId: String): Option[Member]
	def getAllMembersWithUniversityIds(universityIds: Seq[String]): Seq[Member]
	def getMemberByPrsCode(prsCode: String): Option[Member]
	def getAllMembersWithUserId(userId: String, disableFilter: Boolean = false): Seq[Member]
	def getMemberByUserId(userId: String, disableFilter: Boolean = false): Option[Member]
	def getStudentBySprCode(sprCode: String): Option[StudentMember]
	def findMembersByQuery(query: String, departments: Seq[Department], userTypes: Set[MemberUserType], isGod: Boolean): Seq[Member]
	def findMembersByDepartment(department: Department, includeTouched: Boolean, userTypes: Set[MemberUserType]): Seq[Member]
	def listMembersUpdatedSince(startDate: DateTime, max: Int): Seq[Member]
	def countStudentsByDepartment(department: Department): Int
	def getStudentsByRoute(route: Route): Seq[StudentMember]
	def getStudentsByRoute(route: Route, academicYear: AcademicYear): Seq[StudentMember]
	def getStudentCourseDetailsByScjCode(scjCode: String): Option[StudentCourseDetails]
}

abstract class AbstractProfileService extends ProfileService with Logging {

	self: MemberDaoComponent with StudentCourseDetailsDaoComponent =>

	var profileIndexService = Wire.auto[ProfileIndexService]

	def getMemberByUniversityId(universityId: String) = transactional(readOnly = true) {
		memberDao.getByUniversityId(universityId)
	}

	def getAllMembersWithUniversityIds(universityIds: Seq[String]) = transactional(readOnly = true) {
		memberDao.getAllWithUniversityIds(universityIds)
	}

	def getAllMembersWithUserId(userId: String, disableFilter: Boolean = false) = transactional(readOnly = true) {
		memberDao.getAllByUserId(userId, disableFilter)
	}

	def getMemberByUserId(userId: String, disableFilter: Boolean = false) = transactional(readOnly = true) {
		memberDao.getByUserId(userId, disableFilter)
	}

	def getStudentBySprCode(sprCode: String) = transactional(readOnly = true) {
		studentCourseDetailsDao.getStudentBySprCode(sprCode)
	}

	def getMemberByPrsCode(prsCode: String) = transactional(readOnly = true) {
		if (prsCode != null && prsCode.length() > 2) {
			PrsCode.getUniversityId(prsCode) match {
				case Some(uniId: String) => memberDao.getByUniversityId(uniId)
				case None => None
			}
		}
		else None
	}

	def findMembersByQuery(query: String, departments: Seq[Department], userTypes: Set[MemberUserType], isGod: Boolean) = transactional(readOnly = true) {
		profileIndexService.find(query, departments, userTypes, isGod)
	}

	def findMembersByDepartment(department: Department, includeTouched: Boolean, userTypes: Set[MemberUserType]) = transactional(readOnly = true) {
		profileIndexService.find(department, includeTouched, userTypes)
	}

	def listMembersUpdatedSince(startDate: DateTime, max: Int) = transactional(readOnly = true) {
		memberDao.listUpdatedSince(startDate, max)
	}

	def save(member: Member) = memberDao.saveOrUpdate(member)

	def saveOrUpdate(relationship: StudentRelationship) = memberDao.saveOrUpdate(relationship)

	def getRegisteredModules(universityId: String): Seq[Module] = transactional(readOnly = true) {
		memberDao.getRegisteredModules(universityId)
	}

  def countStudentsByDepartment(department: Department): Int = transactional(readOnly = true) {
			memberDao.getStudentsByDepartment(department.rootDepartment).count(department.filterRule.matches)
	}

	def getStudentsByRoute(route: Route): Seq[StudentMember] = transactional(readOnly = true) {
		studentCourseDetailsDao.getByRoute(route).filter{s => !s.sprStatus.code.startsWith("P")}.map(_.student)
	}

	def getStudentsByRoute(route: Route, academicYear: AcademicYear): Seq[StudentMember] = transactional(readOnly = true) {
		studentCourseDetailsDao.getByRoute(route)
			.filter{s => !s.sprStatus.code.startsWith("P")}
			.filter(_.studentCourseYearDetails.asScala.exists(s => s.academicYear == academicYear))
			.map(_.student)
	}

	def getStudentCourseDetailsByScjCode(scjCode: String): Option[StudentCourseDetails] =
		studentCourseDetailsDao.getByScjCode(scjCode)

}

trait ProfileServiceComponent {
	def profileService: ProfileService
}

trait AutowiringProfileServiceComponent extends ProfileServiceComponent {
	var profileService = Wire[ProfileService]
}

@Service("profileService")
class ProfileServiceImpl
	extends AbstractProfileService
	with AutowiringMemberDaoComponent
	with AutowiringStudentCourseDetailsDaoComponent
