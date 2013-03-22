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
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.SitsStatus

trait MemberDao {
	def saveOrUpdate(member: Member)
	def saveOrUpdate(rel: StudentRelationship)
	def getByUniversityId(universityId: String): Option[Member]
	def getBySprCode(sprCode: String): Option[StudentMember]
	def getAllByUserId(userId: String, disableFilter: Boolean = false): Seq[Member]
	def getByUserId(userId: String, disableFilter: Boolean = false): Option[Member]
	def listUpdatedSince(startDate: DateTime, max: Int): Seq[Member]
	def listUpdatedSince(startDate: DateTime, department: Department, max: Int): Seq[Member]
	def getRegisteredModules(universityId: String): Seq[Module]
	def getCurrentRelationship(relationshipType: RelationshipType, targetSprCode: String): Option[StudentRelationship]
	def getRelationshipsByTarget(relationshipType: RelationshipType, targetSprCode: String): Seq[StudentRelationship]
	def getRelationshipsByStudent(relationshipType: RelationshipType, student: StudentMember): Seq[StudentRelationship]
	def getRelationshipsByDepartment(relationshipType: RelationshipType, department: Department): Seq[StudentRelationship]
	def getRelationshipsByAgent(relationshipType: RelationshipType, agentId: String): Seq[StudentRelationship]
	def getStudentsWithoutRelationshipByDepartment(relationshipType: RelationshipType, department: Department): Seq[Member]
	def countStudentsByDepartment(department: Department): Number
	def countStudentsByRelationshipAndDepartment(relationshipType: RelationshipType, department: Department): Number
}

@Repository
class MemberDaoImpl extends MemberDao with Daoisms {
	import Restrictions._
	import Order._
	
	def saveOrUpdate(member: Member) = session.saveOrUpdate(member)
	def saveOrUpdate(rel: StudentRelationship) = session.saveOrUpdate(rel)
	
	def getByUniversityId(universityId: String) = 
		session.newCriteria[Member]
			.add(is("universityId", universityId.trim))
			.uniqueResult
	
	def getBySprCode(sprCode: String) = 
		session.newCriteria[StudentMember]
				.createAlias("studyDetails", "studyDetails")
				.add(is("studyDetails.sprCode", sprCode.trim))
				.uniqueResult
	
	def getAllByUserId(userId: String, disableFilter: Boolean = false) = {
		val filterEnabled = Option(session.getEnabledFilter(Member.StudentsOnlyFilter)).isDefined
		try {
			if (disableFilter) 
				session.disableFilter(Member.StudentsOnlyFilter)
				
			session.newCriteria[Member]
					.add(is("userId", userId.trim.toLowerCase))
					.add(disjunction()
						.add(is("inUseFlag", "Active"))
						.add(like("inUseFlag", "Inactive - Starts %"))
					)
					.addOrder(asc("universityId"))
					.seq
		} finally {
			if (disableFilter && filterEnabled)
				session.enableFilter(Member.StudentsOnlyFilter)
		}
	}
	
	def getByUserId(userId: String, disableFilter: Boolean = false) = getAllByUserId(userId, disableFilter).headOption
	
	def listUpdatedSince(startDate: DateTime, department: Department, max: Int) = 
		session.newCriteria[Member]
				.add(gt("lastUpdatedDate", startDate))
				.add(is("homeDepartment", department))
				.setMaxResults(max)
				.addOrder(asc("lastUpdatedDate"))
				.list
	
	def listUpdatedSince(startDate: DateTime, max: Int) = 
		session.newCriteria[Member].add(gt("lastUpdatedDate", startDate)).setMaxResults(max).addOrder(asc("lastUpdatedDate")).list
	
	def getRegisteredModules(universityId: String): Seq[Module] =		
		session.newQuery[Module]("""
				 select distinct m from Module m where code in 
				(select distinct substring(lower(uag.moduleCode),1,5)
					from UpstreamAssessmentGroup uag
				  where :universityId in elements(uag.members.staticIncludeUsers))
				""")
					.setString("universityId", universityId)
					.seq
	
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
	
	def getRelationshipsByTarget(relationshipType: RelationshipType, targetSprCode: String): Seq[StudentRelationship] = {
			session.newCriteria[StudentRelationship]
					.add(is("targetSprCode", targetSprCode))
					.add(is("relationshipType", relationshipType))
					.seq
	}	
	
	def getRelationshipsByStudent(relationshipType: RelationshipType, student: StudentMember): Seq[StudentRelationship] = {
		session.newQuery[StudentRelationship]("""
			select
				distinct sr
			from
				StudentRelationship sr,
				Member m
			where
				sr.targetSprCode = m.studyDetails.sprCode
			and
				sr.relationshipType = :relationshipType
			and
				m = :student
		""")
			.setEntity("student", student)
			.setParameter("relationshipType", relationshipType)
			.seq
	}	
	
	def getRelationshipsByDepartment(relationshipType: RelationshipType, department: Department): Seq[StudentRelationship] =
		// order by agent to separate any named (external) from numeric (member) agents
		// then by student properties
		session.newQuery[StudentRelationship]("""
			select
				distinct sr
			from
				StudentRelationship sr,
				Member m
			where
				sr.targetSprCode = m.studyDetails.sprCode
			and
				sr.relationshipType = :relationshipType
			and
				m.homeDepartment = :department
			and
				(sr.endDate is null or sr.endDate >= SYSDATE)
			order by
				sr.agent, sr.targetSprCode
		""")
			.setEntity("department", department)
			.setParameter("relationshipType", relationshipType)
			.seq

	def getRelationshipsByAgent(relationshipType: RelationshipType, agentId: String): Seq[StudentRelationship] =
		session.newCriteria[StudentRelationship]
			.add(is("agent", agentId))
			.add(is("relationshipType", relationshipType))
			.add( Restrictions.or(
				Restrictions.isNull("endDate"),
				Restrictions.ge("endDate", new DateTime())
			))
			.seq

	def getStudentsWithoutRelationshipByDepartment(relationshipType: RelationshipType, department: Department): Seq[Member] =
		if (relationshipType == null) Seq()
		else session.newQuery[Member]("""
			select
				distinct sm
			from
				StudentMember sm
			where
				sm.homeDepartment = :department
			and
				sm.studyDetails.sprCode not in (select sr.targetSprCode from StudentRelationship sr where sr.relationshipType = :relationshipType)
		""")
			.setEntity("department", department)
			.setParameter("relationshipType", relationshipType)
			.seq

	def countStudentsByDepartment(department: Department): Number =
		if (department == null) 0
		else session.newQuery[Number]("""
			select
				count(*)
			from
				StudentMember sm
			where
				sm.homeDepartment = :department
			and
				sm.studyDetails.enrolmentStatus != null && sm.studyDetails.getRoute != null
		""")
			.setEntity("department", department)
			.uniqueResult.getOrElse(0)

	def countStudentsByRelationshipAndDepartment(relationshipType: RelationshipType, department: Department): Number =
		if (relationshipType == null) 0
		else session.newQuery[Number]("""
			select
				count(*)
			from
				StudentMember sm
			where
				sm.homeDepartment = :department
			and
				sm.studyDetails.sprCode not in (select sr.targetSprCode from StudentRelationship sr where sr.relationshipType = :relationshipType)
			and
				sm.studyDetails.enrolmentStatus != null && sm.studyDetails.getRoute != null
		""")
			.setEntity("department", department)
			.setParameter("relationshipType", relationshipType)
			.uniqueResult.getOrElse(0)
}
