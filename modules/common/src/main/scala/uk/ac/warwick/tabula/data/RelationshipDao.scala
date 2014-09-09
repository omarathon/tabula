package uk.ac.warwick.tabula.data

import org.hibernate.FetchMode
import org.hibernate.criterion.Order
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.Projections
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.model.MemberStudentRelationship

trait RelationshipDaoComponent {
	val relationshipDao: RelationshipDao
}

trait AutowiringRelationshipDaoComponent extends RelationshipDaoComponent {
	val relationshipDao = Wire[RelationshipDao]
}

trait RelationshipDao {
	def allStudentRelationshipTypes: Seq[StudentRelationshipType]
	def getStudentRelationshipTypeById(id: String): Option[StudentRelationshipType]
	def getStudentRelationshipTypeByUrlPart(urlPart: String): Option[StudentRelationshipType]
	def saveOrUpdate(relationshipType: StudentRelationshipType)
	def delete(relationshipType: StudentRelationshipType)
	def getStudentRelationshipById(id: String): Option[StudentRelationship]

	def saveOrUpdate(rel: StudentRelationship)

	def getAllCurrentRelationships(student: StudentMember): Seq[StudentRelationship]
	def getAllPastAndPresentRelationships(student: StudentMember): Seq[StudentRelationship]
	def getCurrentRelationships(relationshipType: StudentRelationshipType, scd: StudentCourseDetails): Seq[StudentRelationship]
	def getCurrentRelationships(relationshipType: StudentRelationshipType, student: StudentMember): Seq[StudentRelationship]
	def getRelationshipsByTarget(relationshipType: StudentRelationshipType, student: StudentMember): Seq[StudentRelationship]
	def getRelationshipsByDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentRelationship]
	def getRelationshipsByStaffDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentRelationship]
	def getAllRelationshipsByAgent(agentId: String): Seq[MemberStudentRelationship]
	def getAllRelationshipTypesByStudent(student: StudentMember): Seq[StudentRelationshipType]
	def getAllRelationshipTypesByAgent(agentId: String): Seq[StudentRelationshipType]
	def getRelationshipsByAgent(relationshipType: StudentRelationshipType, agentId: String): Seq[MemberStudentRelationship]
	def getStudentsWithoutRelationshipByDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentMember]
	def getStudentsByRelationshipAndDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentMember]
	def countStudentsByRelationship(relationshipType: StudentRelationshipType): Number
	def getAllPastAndPresentRelationships(relationshipType: StudentRelationshipType, scd: StudentCourseDetails): Seq[StudentRelationship]

}

@Repository
class RelationshipDaoImpl extends RelationshipDao with Daoisms with Logging {
	import Restrictions._
	import Order._
	import Projections._

	def allStudentRelationshipTypes: Seq[StudentRelationshipType] =
		session.newCriteria[StudentRelationshipType]
			.addOrder(Order.asc("sortOrder"))
			.addOrder(Order.asc("id"))
			.seq

	def getStudentRelationshipTypeById(id: String) = getById[StudentRelationshipType](id)

	def getStudentRelationshipTypeByUrlPart(urlPart: String) =
		session.newCriteria[StudentRelationshipType]
			.add(is("urlPart", urlPart))
			.uniqueResult

	def saveOrUpdate(relationshipType: StudentRelationshipType) = session.saveOrUpdate(relationshipType)
	def delete(relationshipType: StudentRelationshipType) = session.delete(relationshipType)

	def getStudentRelationshipById(id: String) = getById[StudentRelationship](id)

	def saveOrUpdate(rel: StudentRelationship) = session.saveOrUpdate(rel)

	def getAllCurrentRelationships(student: StudentMember): Seq[StudentRelationship] = {
		session.newCriteria[StudentRelationship]
			.createAlias("studentCourseDetails", "scd")
			.add(is("scd.student", student))
			.add( Restrictions.or(
			Restrictions.isNull("endDate"),
			Restrictions.ge("endDate", new DateTime())
		))
			.seq
	}

	def getAllPastAndPresentRelationships(student: StudentMember): Seq[StudentRelationship] = {
		session.newCriteria[StudentRelationship]
			.createAlias("studentCourseDetails", "scd")
			.add(is("scd.student", student))
			.seq
	}

	def getAllPastAndPresentRelationships(relationshipType: StudentRelationshipType, scd: StudentCourseDetails): Seq[StudentRelationship] = {
		session.newCriteria[StudentRelationship]
			.add(is("studentCourseDetails", scd))
			.add(is("relationshipType", relationshipType))
			.seq
	}

	def getCurrentRelationships(relationshipType: StudentRelationshipType, student: StudentMember): Seq[StudentRelationship] = {
		session.newCriteria[StudentRelationship]
			.createAlias("studentCourseDetails", "scd")
			.add(is("scd.student", student))
			.add(is("relationshipType", relationshipType))
			.add( Restrictions.or(
			Restrictions.isNull("endDate"),
			Restrictions.ge("endDate", new DateTime())
		))
			.seq
	}

	def getCurrentRelationships(relationshipType: StudentRelationshipType, scd: StudentCourseDetails): Seq[StudentRelationship] = {
		session.newCriteria[StudentRelationship]
			.add(is("studentCourseDetails", scd))
			.add(is("relationshipType", relationshipType))
			.add( Restrictions.or(
			Restrictions.isNull("endDate"),
			Restrictions.ge("endDate", new DateTime())
		))
			.seq
	}

	def getRelationshipsByTarget(relationshipType: StudentRelationshipType, student: StudentMember): Seq[StudentRelationship] = {
		session.newCriteria[StudentRelationship]
			.createAlias("studentCourseDetails", "scd")
			.add(is("scd.student", student))
			.add(is("relationshipType", relationshipType))
			.seq
	}

	def getRelationshipsByDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentRelationship] = {
		// This eagerly fetches a few associations for the view-students-by-tutor page in Attendance Monitoring
		// (/attendance/view/[dept]/agents/[reltype]) - TAB-1868

		session.newCriteria[StudentRelationship]
			.createAlias("studentCourseDetails", "studentCourseDetails")
			.createAlias("studentCourseDetails.student", "student")
			.add(is("relationshipType", relationshipType))
			.add(is("studentCourseDetails.department", department))
			.add(is("studentCourseDetails.mostSignificant", true))
			.add(not(like("studentCourseDetails.statusOnRoute.code", "P%")))
			.add(or(isNull("endDate"), ge("endDate", DateTime.now)))
			.addOrder(asc("_agentMember"))
			.addOrder(asc("studentCourseDetails"))
			.setFetchMode("studentCourseDetails.student", FetchMode.JOIN)
			.setFetchMode("studentCourseDetails.route", FetchMode.JOIN)
			.setFetchMode("studentCourseDetails.latestYearDetails", FetchMode.JOIN)
			.setFetchMode("studentCourseDetails.latestYearDetails.enrolmentStatus", FetchMode.JOIN)
			.setFetchMode("_agentMember", FetchMode.JOIN)
			.seq
	}

	def getRelationshipsByStaffDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentRelationship] = {
		session.newQuery[StudentRelationship]("""
			select
				distinct sr
			from
				StudentRelationship sr,
				StudentCourseDetails scd,
				Member staff
			where
				sr.studentCourseDetails = scd
      and
        staff = sr._agentMember
			and
				sr.relationshipType = :relationshipType
			and
				staff.homeDepartment = :department
			and
				scd.statusOnRoute.code not like 'P%'
			and
				(sr.endDate is null or sr.endDate >= SYSDATE)
			order by
				sr._agentMember, sr.studentCourseDetails
																					""")
			.setEntity("department", department)
			.setEntity("relationshipType", relationshipType)
			.seq
	}


	def getAllRelationshipsByAgent(agentId: String): Seq[MemberStudentRelationship] =
		session.newCriteria[MemberStudentRelationship]
			.add(is("_agentMember.universityId", agentId))
			.add( Restrictions.or(
			Restrictions.isNull("endDate"),
			Restrictions.ge("endDate", new DateTime())
		))
			.seq

	def getAllRelationshipTypesByStudent(student: StudentMember): Seq[StudentRelationshipType] =
		session.newCriteria[StudentRelationship]
			.createAlias("studentCourseDetails", "scd")
			.add(is("scd.student", student))
			.add( Restrictions.or(
			Restrictions.isNull("endDate"),
			Restrictions.ge("endDate", new DateTime())
		))
			.project[StudentRelationshipType](distinct(property("relationshipType")))
			.seq


	def getAllRelationshipTypesByAgent(agentId: String): Seq[StudentRelationshipType] =
		session.newCriteria[MemberStudentRelationship]
			.add(is("_agentMember.universityId", agentId))
			.add( Restrictions.or(
			Restrictions.isNull("endDate"),
			Restrictions.ge("endDate", new DateTime())
		))
			.project[StudentRelationshipType](distinct(property("relationshipType")))
			.seq

	def getRelationshipsByAgent(relationshipType: StudentRelationshipType, agentId: String): Seq[MemberStudentRelationship] =
		session.newCriteria[MemberStudentRelationship]
			.add(is("_agentMember.universityId", agentId))
			.add(is("relationshipType", relationshipType))
			.add( Restrictions.or(
			Restrictions.isNull("endDate"),
			Restrictions.ge("endDate", new DateTime())
		))
			.seq

	def getStudentsWithoutRelationshipByDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentMember] =
		if (relationshipType == null) Seq()
		else session.newQuery[StudentMember]("""
			select
				distinct sm
			from
				StudentMember sm
				inner join sm.studentCourseDetails as scd
			where
				scd.department = :department
			and
				scd.mostSignificant = true
			and
				scd.statusOnRoute.code not like 'P%'
			and
				scd not in (
					select
						sr.studentCourseDetails
					from
						StudentRelationship sr
					where
						sr.relationshipType = :relationshipType
					and
						(sr.endDate is null or sr.endDate >= SYSDATE)
				)
																				 """)
			.setEntity("department", department)
			.setEntity("relationshipType", relationshipType)
			.seq

	/**
	 * n.b. this will only return students with a direct relationship to a department. For sub-department memberships,
	 * see ProfileService/RelationshipService
	 */
	def getStudentsByRelationshipAndDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentMember]=
		if (relationshipType == null) Nil
		else session.newQuery[StudentMember]("""
			select distinct student
			from
				StudentCourseDetails scd
			where
				scd.department = :department
			and
				scd.mostSignificant = true
			and
				scd.statusOnRoute.code not like 'P%'
			and
				scd in (select sr.studentCourseDetails from StudentRelationship sr where sr.relationshipType = :relationshipType)
																				 """)
			.setEntity("department", department)
			.setEntity("relationshipType", relationshipType)
			.seq


	def countStudentsByRelationship(relationshipType: StudentRelationshipType): Number =
		if (relationshipType == null) 0
		else session.newQuery[Number]("""
			select
				count(distinct student)
			from
				StudentCourseDetails scd
			where
				scd in (select sr.studentCourseDetails from StudentRelationship sr where sr.relationshipType = :relationshipType)
																	""")
			.setEntity("relationshipType", relationshipType)
			.uniqueResult.getOrElse(0)
}

