package uk.ac.warwick.tabula.services

import org.joda.time.DateTime
import org.springframework.stereotype.Service

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.MemberUserType
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.RelationshipType._
import uk.ac.warwick.tabula.data.model.RelationshipType
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.StudentMember

/**
 * Service providing access to members and profiles.
 */
trait ProfileService {
	def save(member: Member)
	def getRegisteredModules(universityId: String): Seq[Module]
	def getMemberByUniversityId(universityId: String): Option[Member]
	def getAllMembersWithUserId(userId: String, disableFilter: Boolean = false): Seq[Member]
	def getMemberByUserId(userId: String, disableFilter: Boolean = false): Option[Member]
	def getStudentBySprCode(sprCode: String): Option[StudentMember]
	def findMembersByQuery(query: String, departments: Seq[Department], userTypes: Set[MemberUserType], isGod: Boolean): Seq[Member]
	def findMembersByDepartment(department: Department, includeTouched: Boolean, userTypes: Set[MemberUserType]): Seq[Member]
	def listMembersUpdatedSince(startDate: DateTime, max: Int): Seq[Member]
	def findCurrentRelationship(relationshipType: RelationshipType, targetUniversityId: String): Option[StudentRelationship]
	def saveStudentRelationship(relationshipType: RelationshipType, targetSprCode: String, agent: String): StudentRelationship
	def listStudentRelationshipsByDepartment(relationshipType: RelationshipType, department: Department): Seq[StudentRelationship]
	def listStudentRelationshipsWithMember(relationshipType: RelationshipType, agent: Member): Seq[StudentRelationship]
	def getPersonalTutor(student: Member): Option[Member]
	def listStudentRelationshipsWithUniversityId(relationshipType: RelationshipType, agentId: String): Seq[StudentRelationship]
	def listStudentsWithoutRelationship(relationshipType: RelationshipType, department: Department): Seq[Member]
}

@Service(value = "profileService")
class ProfileServiceImpl extends ProfileService with Logging {
	
	var memberDao = Wire.auto[MemberDao]
	var profileIndexService = Wire.auto[ProfileIndexService]
	
	def getMemberByUniversityId(universityId: String) = transactional(readOnly = true) {
		memberDao.getByUniversityId(universityId)
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
	
	def getRegisteredModules(universityId: String): Seq[Module] = transactional(readOnly = true) {
		memberDao.getRegisteredModules(universityId)
	}

	def findCurrentRelationship(relationshipType: RelationshipType, targetSprCode: String): Option[StudentRelationship] = transactional() {
		memberDao.getCurrentRelationship(relationshipType, targetSprCode)
	}
	
	def getRelationships(relationshipType: RelationshipType, targetSprCode: String): Seq[StudentRelationship] = transactional(readOnly = true) {
		memberDao.getRelationshipsByTarget(relationshipType, targetSprCode)
	}
	
	def getPersonalTutor(student: Member): Option[Member] = {
		student match {
			case student: StudentMember => {
				val sprCode = student.studyDetails.sprCode
				val currentRelationship = findCurrentRelationship(PersonalTutor, sprCode)
				currentRelationship.flatMap { rel => getMemberByUniversityId(rel.agent) }
			}
			case _ => None
		}
	}
	
	def saveStudentRelationship(relationshipType: RelationshipType, targetSprCode: String, agent: String): StudentRelationship = transactional() {
		val currentRelationship = this.findCurrentRelationship(PersonalTutor, targetSprCode)
		currentRelationship match {
			case None => {
				val newRelationship = StudentRelationship(agent, PersonalTutor, targetSprCode)
				memberDao.saveOrUpdate(newRelationship)
				newRelationship
			}
			case Some(existingRelationship) => {
				if (existingRelationship.agent.equals(agent)) {
					// the same relationship is already there in the db - don't save
					existingRelationship
				}
				else {
					// set the end date of the existing personal tutor relationship to now
					existingRelationship.endDate = new DateTime
					memberDao.saveOrUpdate(existingRelationship)
					
					// and then create the new one
					val newRelationship = StudentRelationship(agent, PersonalTutor, targetSprCode)
					newRelationship.startDate = new DateTime
					memberDao.saveOrUpdate(newRelationship)
					newRelationship
				}
			}
		}
	}
	
	def listStudentRelationshipsByDepartment(relationshipType: RelationshipType, department: Department): Seq[StudentRelationship] = transactional() {
		memberDao.getRelationshipsByDepartment(relationshipType, department)
	}

	def listStudentRelationshipsWithMember(relationshipType: RelationshipType, agent: Member): Seq[StudentRelationship] = transactional() {
		memberDao.getRelationshipsByAgent(relationshipType, agent.universityId)
	}

	def listStudentRelationshipsWithUniversityId(relationshipType: RelationshipType, agentId: String): Seq[StudentRelationship] = transactional() {
		memberDao.getRelationshipsByAgent(relationshipType, agentId)
	}

  def listStudentsWithoutRelationship(relationshipType: RelationshipType, department: Department): Seq[Member] = transactional() {
		memberDao.getStudentsWithoutRelationshipByDepartment(relationshipType, department)
	}
}
