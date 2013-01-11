package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.data.model.Member
import org.springframework.stereotype.Repository
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.Order
import org.joda.time.DateTime
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model.RelationshipType
import uk.ac.warwick.tabula.data.model.MemberRelationship

trait MemberDao {
	def saveOrUpdate(member: Member)
	def getByUniversityId(universityId: String): Option[Member]
	def getByUserId(userId: String, disableFilter: Boolean = false): Option[Member]
	def findByQuery(query: String): Seq[Member]
	def listUpdatedSince(startDate: DateTime, max: Int): Seq[Member]
	def getRegisteredModules(universityId: String): Seq[Module]
	def getRelationship(relationshipType: RelationshipType, subjectUniversityId: String): Option[MemberRelationship]
}

@Repository
class MemberDaoImpl extends MemberDao with Daoisms {
	import Restrictions._
	import Order._
	
	def saveOrUpdate(member: Member) = session.saveOrUpdate(member)

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
		session.createSQLQuery("""
				 select distinct ua.modulecode from upstreamassignment  ua
                   join upstreamassessmentgroup uag 
                    on uag.modulecode = ua.modulecode 
                    and uag.assessmentgroup = ua.assessmentgroup
                  join usergroupstatic ugs on uag.membersgroup_id = ugs.group_id
                   where ugs.usercode = :universityId
				""")
				
		    .addEntity(classOf[Module])
            .setString("universityId", universityId)
            .list.asInstanceOf[JList[Module]]
	}
	
	def getRelationship(relationshipType: RelationshipType, subjectUniversityId: String): Option[MemberRelationship] = {
			session.newCriteria[MemberRelationship]
					.add(is("subjectUniversityId", subjectUniversityId))
					.add(is("relationshipType", relationshipType))	
					.uniqueResult
	}		

	
}
