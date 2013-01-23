package uk.ac.warwick.tabula.services

import org.hibernate.annotations.AccessType
import org.hibernate.annotations.FilterDefs
import org.hibernate.annotations.Filters
import org.joda.time.DateTime
import org.springframework.stereotype.Service

import javax.persistence.Entity
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.MemberUserType
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.PersonalTutor
import uk.ac.warwick.tabula.data.model.RelationshipType
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.helpers.Logging

/**
 * Service providing access to members and profiles.
 */
trait ProfileService {
	def save(member: Member)
	def getRegisteredModules(universityId: String): Seq[Module]
	def getMemberByUniversityId(universityId: String): Option[Member]
	def getMemberByUserId(userId: String, disableFilter: Boolean = false): Option[Member]
	def findMembersByQuery(query: String, departments: Seq[Department], userTypes: Set[MemberUserType], isGod: Boolean): Seq[Member]
	def findMembersByDepartment(department: Department, userTypes: Set[MemberUserType]): Seq[Member]
	def listMembersUpdatedSince(startDate: DateTime, max: Int): Seq[Member]
	def findCurrentRelationship(relationshipType: RelationshipType, targetUniversityId: String): Option[StudentRelationship]
	def saveStudentRelationship(relationshipType: RelationshipType, targetSprCode: String, agent: String): StudentRelationship
}

@Service(value = "profileService")
class ProfileServiceImpl extends ProfileService with Logging {
	
	var memberDao = Wire.auto[MemberDao]
	var profileIndexService = Wire.auto[ProfileIndexService]
	
	def getMemberByUniversityId(universityId: String) = transactional(readOnly = true) {
		memberDao.getByUniversityId(universityId)
	}
	
	def getMemberByUserId(userId: String, disableFilter: Boolean = false) = transactional(readOnly = true) {
		memberDao.getByUserId(userId, disableFilter)
	}
	
	def findMembersByQuery(query: String, departments: Seq[Department], userTypes: Set[MemberUserType], isGod: Boolean) = transactional(readOnly = true) {
		profileIndexService.find(query, departments, userTypes, isGod)
	} 
	
	def findMembersByDepartment(department: Department, userTypes: Set[MemberUserType]) = transactional(readOnly = true) {
		profileIndexService.find(department, userTypes)
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
		memberDao.getRelationships(relationshipType, targetSprCode)
	}
	
	def saveStudentRelationship(relationshipType: RelationshipType, targetSprCode: String, agent: String): StudentRelationship = {
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

}
