package uk.ac.warwick.tabula.services

import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.MemberUserType
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.RelationshipType._
import uk.ac.warwick.tabula.data.model.RelationshipType
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.PrsCode
import uk.ac.warwick.tabula.data.StudentCourseDetailsDao

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
}

@Service(value = "profileService")
class ProfileServiceImpl extends ProfileService with Logging {

	var memberDao = Wire.auto[MemberDao]
	var profileIndexService = Wire.auto[ProfileIndexService]
	var studentCourseDetailsDao = Wire.auto[StudentCourseDetailsDao]

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
		val studentCourseDetails = studentCourseDetailsDao.getBySprCode(sprCode)
		studentCourseDetails.map { _.student }
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
		memberDao.countStudentsByDepartment(department).intValue
	}
}
