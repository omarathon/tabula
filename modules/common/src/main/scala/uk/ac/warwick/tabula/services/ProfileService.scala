package uk.ac.warwick.tabula.services

import scala.collection.JavaConversions._
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.persistence.Entity
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.userlookup.User
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.Order
import uk.ac.warwick.tabula.helpers.{ FoundUser, Logging }
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.spring.Wire
import org.joda.time.DateTime

/**
 * Service providing access to members and profiles.
 */
trait ProfileService {
	def save(member: Member)
	def getRegisteredModules(universityId: String): Seq[Module]
	def getMemberByUniversityId(universityId: String): Option[Member]
	def getMemberByUserId(userId: String, disableFilter: Boolean = false): Option[Member]
	def findMembersByQuery(query: String, departments: Seq[Department], userTypes: Set[MemberUserType], sysAdmin: Boolean): Seq[Member]
	def listMembersUpdatedSince(startDate: DateTime, max: Int): Seq[Member]
	def findRelationship(relationshipType: RelationshipType, subjectUniversityId: String): Option[MemberRelationship]
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
	
	def findMembersByQuery(query: String, departments: Seq[Department], userTypes: Set[MemberUserType], sysAdmin: Boolean) = transactional(readOnly = true) {
		profileIndexService.find(query, departments, userTypes, sysAdmin)
	} 
	
	def listMembersUpdatedSince(startDate: DateTime, max: Int) = transactional(readOnly = true) {
		memberDao.listUpdatedSince(startDate, max)
	}
	
	def save(member: Member) = memberDao.saveOrUpdate(member)
	
	def getRegisteredModules(universityId: String): Seq[Module] = transactional(readOnly = true) {
		memberDao.getRegisteredModules(universityId)
	}
	
	def findRelationship(relationshipType: RelationshipType, subjectUniversityId: String): Option[MemberRelationship] = transactional() {
		memberDao.getRelationship(relationshipType, subjectUniversityId)
	}
}
