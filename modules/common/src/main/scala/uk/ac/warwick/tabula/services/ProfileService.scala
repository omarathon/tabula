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

/**
 * Service providing access to members and profiles.
 */
trait ProfileService {
	def save(member: Member)
	def saveOrUpdate(relationship: StudentRelationship)
	def getRegisteredModules(universityId: String): Seq[Module]
	def getMemberByUniversityId(universityId: String): Option[Member]
	def getAllMembersWithUniversityIds(universityIds: Seq[String]): Seq[Member]
	def getAllMembersWithUserId(userId: String, disableFilter: Boolean = false): Seq[Member]
	def getMemberByUserId(userId: String, disableFilter: Boolean = false): Option[Member]
	def getStudentBySprCode(sprCode: String): Option[StudentMember]
	def findMembersByQuery(query: String, departments: Seq[Department], userTypes: Set[MemberUserType], isGod: Boolean): Seq[Member]
	def findMembersByDepartment(department: Department, includeTouched: Boolean, userTypes: Set[MemberUserType]): Seq[Member]
	def listMembersUpdatedSince(startDate: DateTime, max: Int): Seq[Member]
	def findCurrentRelationships(relationshipType: RelationshipType, targetSprCode: String): Seq[StudentRelationship]
	def getRelationships(relationshipType: RelationshipType, targetUniversityId: String): Seq[StudentRelationship]
	def getRelationships(relationshipType: RelationshipType, student: StudentMember): Seq[StudentRelationship]
	def saveStudentRelationship(relationshipType: RelationshipType, targetSprCode: String, agent: String): StudentRelationship
	def listStudentRelationshipsByDepartment(relationshipType: RelationshipType, department: Department): Seq[StudentRelationship]
	def listStudentRelationshipsWithMember(relationshipType: RelationshipType, agent: Member): Seq[StudentRelationship]
	def getPersonalTutors(student: Member): Seq[Member]
	def listStudentRelationshipsWithUniversityId(relationshipType: RelationshipType, agentId: String): Seq[StudentRelationship]
	def listStudentsWithoutRelationship(relationshipType: RelationshipType, department: Department): Seq[Member]
	def countStudentsByDepartment(department: Department): Int
	def countStudentsByRelationshipAndDepartment(relationshipType: RelationshipType, department: Department): (Int, Int)
}

@Service(value = "profileService")
class ProfileServiceImpl extends ProfileService with Logging {
	
	var memberDao = Wire.auto[MemberDao]
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
		memberDao.getBySprCode(sprCode)
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

	def findCurrentRelationships(relationshipType: RelationshipType, targetSprCode: String): Seq[StudentRelationship] = transactional() {
		memberDao.getCurrentRelationships(relationshipType, targetSprCode)
	}
	
	def getRelationships(relationshipType: RelationshipType, targetSprCode: String): Seq[StudentRelationship] = transactional(readOnly = true) {
		memberDao.getRelationshipsByTarget(relationshipType, targetSprCode)
	}
	
	def getRelationships(relationshipType: RelationshipType, student: StudentMember): Seq[StudentRelationship] = transactional(readOnly = true) {
		memberDao.getRelationshipsByStudent(relationshipType, student)
	}
	
	def getPersonalTutors(student: Member): Seq[Member] = {
		student match {
			case student: StudentMember => {
				val sprCode = student.studyDetails.sprCode
				val currentRelationship = findCurrentRelationships(PersonalTutor, sprCode)
				currentRelationship.flatMap { rel => getMemberByUniversityId(rel.agent) }
			}
			case _ => Nil
		}
	}
	
	def saveStudentRelationship(relationshipType: RelationshipType, targetSprCode: String, agent: String): StudentRelationship = transactional() {
		this.findCurrentRelationships(PersonalTutor, targetSprCode).find(_.agent == agent) match {
			case Some(existingRelationship) => {
				// the same relationship is already there in the db - don't save
				existingRelationship
			}
			case _ => {
				// TODO handle existing relationships?
				// and then create the new one
				val newRelationship = StudentRelationship(agent, PersonalTutor, targetSprCode)
				newRelationship.startDate = new DateTime
				memberDao.saveOrUpdate(newRelationship)
				newRelationship
			}
		}
	}
	
	def listStudentRelationshipsByDepartment(relationshipType: RelationshipType, department: Department) = transactional(readOnly=true) {
		memberDao.getRelationshipsByDepartment(relationshipType, department)
	}

	def listStudentRelationshipsWithMember(relationshipType: RelationshipType, agent: Member) = transactional(readOnly = true) {
		memberDao.getRelationshipsByAgent(relationshipType, agent.universityId)
	}

	def listStudentRelationshipsWithUniversityId(relationshipType: RelationshipType, agentId: String) = transactional(readOnly = true) {
		memberDao.getRelationshipsByAgent(relationshipType, agentId)
	}

  def listStudentsWithoutRelationship(relationshipType: RelationshipType, department: Department) = transactional(readOnly = true) {
		memberDao.getStudentsWithoutRelationshipByDepartment(relationshipType, department)
	}

  def countStudentsByDepartment(department: Department): Int = transactional(readOnly = true) {
		memberDao.countStudentsByDepartment(department).intValue
	}

  def countStudentsByRelationshipAndDepartment(relationshipType: RelationshipType, department: Department): (Int, Int) = transactional(readOnly = true) {
		(memberDao.countStudentsByDepartment(department).intValue, memberDao.countStudentsByRelationshipAndDepartment(relationshipType, department).intValue)
	}
}
