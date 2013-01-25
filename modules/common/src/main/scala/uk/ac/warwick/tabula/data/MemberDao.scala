package uk.ac.warwick.tabula.data

import scala.collection.JavaConversions.asScalaBuffer
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.FilterDefs
import org.hibernate.annotations.Filters
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.Restrictions.gt
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import javax.persistence.Entity
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.RelationshipType
import uk.ac.warwick.tabula.data.model.StudentRelationship
import org.hibernate.criterion._

trait MemberDao {
	def saveOrUpdate(member: Member)
	def saveOrUpdate(rel: StudentRelationship)
	def getByUniversityId(universityId: String): Option[Member]
	def getByUserId(userId: String, disableFilter: Boolean = false): Option[Member]
	def findByQuery(query: String): Seq[Member]
	def listUpdatedSince(startDate: DateTime, max: Int): Seq[Member]
	def getRegisteredModules(universityId: String): Seq[Module]
	def getCurrentRelationship(relationshipType: RelationshipType, targetSprCode: String): Option[StudentRelationship]
	def getRelationships(relationshipType: RelationshipType, targetSprCode: String): Seq[StudentRelationship]
}

@Repository
class MemberDaoImpl extends MemberDao with Daoisms {
	import Restrictions._
	import Order._
	
	def saveOrUpdate(member: Member) = session.saveOrUpdate(member)
	def saveOrUpdate(rel: StudentRelationship) = session.saveOrUpdate(rel)
	
	def getByUniversityId(universityId: String) = 
		session.newCriteria[Member].add(is("universityId", universityId.trim)).uniqueResult
	
	def getByUserId(userId: String, disableFilter: Boolean = false) = {
		val filterEnabled = Option(session.getEnabledFilter(Member.StudentsOnlyFilter)).isDefined
		try {
			if (disableFilter) 
				session.disableFilter(Member.StudentsOnlyFilter)
				
			session.newCriteria[Member]
					.add(is("userId", userId.trim.toLowerCase))
					.addOrder(asc("universityId"))
					.list.headOption
		} finally {
			if (disableFilter && filterEnabled)
				session.enableFilter(Member.StudentsOnlyFilter)
		}
	}
	
	def listUpdatedSince(startDate: DateTime, max: Int) = 
		session.newCriteria[Member].add(gt("lastUpdatedDate", startDate)).setMaxResults(max).addOrder(asc("lastUpdatedDate")).list
	
	//TODO
	def findByQuery(query: String) = Seq()
	
	def getRegisteredModules(universityId: String): Seq[Module] = {
		session.createQuery("""
				 select distinct m from Module m where code in 
				(select distinct substring(lower(uag.moduleCode),1,5)
					from UpstreamAssessmentGroup as uag
				join uag.members as usergroup
				join usergroup.staticIncludeUsers as uniId
				where uniId = :universityId)
				""")
					.setString("universityId", universityId)
					.list.asInstanceOf[JList[Module]]
	}
	
	def getCurrentRelationship(relationshipType: RelationshipType, targetSprCode: String): Option[StudentRelationship] = {
			session.newCriteria[StudentRelationship]
					.add(is("targetSprCode", targetSprCode))
					.add(is("relationshipType", relationshipType))
					.add( Restrictions.or(
							Restrictions.isNull("endDate"),
							Restrictions.ge("endDate", new DateTime())
							))				
					.uniqueResult
	}
	
	def getRelationships(relationshipType: RelationshipType, targetSprCode: String): Seq[StudentRelationship] = {
			session.newCriteria[StudentRelationship]
					.add(is("targetSprCode", targetSprCode))
					.add(is("relationshipType", relationshipType))
					.seq
					//.list.asInstanceOf[JList[StudentRelationship]]
	}	
}
