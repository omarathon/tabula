package uk.ac.warwick.tabula.data

import scala.collection.JavaConversions.{asScalaBuffer, seqAsJavaList}
import org.hibernate.criterion.Order
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Restrictions
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Department, Member, ModeOfAttendance, RuntimeMember, SitsStatus, StudentMember, StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering.orderedDateTime
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils.StringToSuperString
import uk.ac.warwick.tabula.data.model.StaffMember
import org.hibernate.FetchMode

trait MemberDaoComponent {
	val memberDao: MemberDao
}

trait AutowiringMemberDaoComponent extends MemberDaoComponent {
	val memberDao = Wire[MemberDao]
}

trait MemberDao {
	def allStudentRelationshipTypes: Seq[StudentRelationshipType]
	def getStudentRelationshipTypeById(id: String): Option[StudentRelationshipType]
	def getStudentRelationshipTypeByUrlPart(urlPart: String): Option[StudentRelationshipType]
	def saveOrUpdate(relationshipType: StudentRelationshipType)
	def delete(relationshipType: StudentRelationshipType)

	def saveOrUpdate(member: Member)
	def delete(member: Member)
	def saveOrUpdate(rel: StudentRelationship)
	def getByUniversityId(universityId: String): Option[Member]
	def getByUniversityIdStaleOrFresh(universityId: String): Option[Member]
	def getAllWithUniversityIds(universityIds: Seq[String]): Seq[Member]
	def getAllWithUniversityIdsStaleOrFresh(universityIds: Seq[String]): Seq[Member]
	def getAllByUserId(userId: String, disableFilter: Boolean = false, eagerLoad: Boolean = false): Seq[Member]
	def listUpdatedSince(startDate: DateTime, max: Int): Seq[Member]
	def listUpdatedSince(startDate: DateTime, department: Department, max: Int): Seq[Member]
	def getAllCurrentRelationships(targetSprCode: String): Seq[StudentRelationship]
	def getCurrentRelationships(relationshipType: StudentRelationshipType, targetSprCode: String): Seq[StudentRelationship]
	def getRelationshipsByTarget(relationshipType: StudentRelationshipType, targetSprCode: String): Seq[StudentRelationship]
	def getRelationshipsByDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentRelationship]
	def getRelationshipsByStaffDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentRelationship]
	def getAllRelationshipsByAgent(agentId: String): Seq[StudentRelationship]
	def getAllRelationshipTypesByAgent(agentId: String): Seq[StudentRelationshipType]
	def getRelationshipsByAgent(relationshipType: StudentRelationshipType, agentId: String): Seq[StudentRelationship]
	def getStudentsWithoutRelationshipByDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentMember]
	def getStudentsByDepartment(department: Department): Seq[StudentMember]
	def getStaffByDepartment(department: Department): Seq[StaffMember]
	def getStudentsByRelationshipAndDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentMember]
	def countStudentsByRelationship(relationshipType: StudentRelationshipType): Number
	def findUniversityIdsByRestrictions(restrictions: Iterable[ScalaRestriction]): Seq[String]
	def findStudentsByRestrictions(restrictions: Iterable[ScalaRestriction], orders: Iterable[ScalaOrder], maxResults: Int, startResult: Int): Seq[StudentMember]
	def countStudentsByRestrictions(restrictions: Iterable[ScalaRestriction]): Int
	def getAllModesOfAttendance(department: Department): Seq[ModeOfAttendance]
	def getAllSprStatuses(department: Department): Seq[SitsStatus]
	def getFreshUniversityIds: Seq[String]
	def stampMissingFromImport(newStaleUniversityIds: Seq[String], importStart: DateTime)

}

@Repository
class MemberDaoImpl extends MemberDao with Daoisms with Logging {
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

	def saveOrUpdate(member: Member) = member match {
		case ignore: RuntimeMember => // shouldn't ever get here, but making sure
		case _ => session.saveOrUpdate(member)
	}

	def delete(member: Member) = member match {
		case ignore: RuntimeMember => // shouldn't ever get here, but making sure
		case _ => {
			session.delete(member)
			// Immediately flush delete
			session.flush()
		}
	}

	def saveOrUpdate(rel: StudentRelationship) = session.saveOrUpdate(rel)

	def getByUniversityId(universityId: String) =
		session.newCriteria[Member]
			.add(is("universityId", universityId.safeTrim))
			.add(isNull("missingFromImportSince"))
			.uniqueResult

	def getByUniversityIdStaleOrFresh(universityId: String) =
		session.newCriteria[Member]
			.add(is("universityId", universityId.safeTrim))
			.uniqueResult

	def getFreshUniversityIds() =
			session.newCriteria[StudentMember]
			.add(isNull("missingFromImportSince"))
			.project[String](Projections.property("universityId"))
			.seq

	def getAllWithUniversityIds(universityIds: Seq[String]) =
		if (universityIds.isEmpty) Seq.empty
		else session.newCriteria[Member]
			.add(in("universityId", universityIds map { _.safeTrim }))
			.add(isNull("missingFromImportSince"))
			.seq
			
	def getAllWithUniversityIdsStaleOrFresh(universityIds: Seq[String]) =
		if (universityIds.isEmpty) Seq.empty
		else session.newCriteria[Member]
			.add(in("universityId", universityIds map { _.safeTrim }))
			.seq

	def getAllByUserId(userId: String, disableFilter: Boolean = false, eagerLoad: Boolean = false) = {
		val filterEnabled = Option(session.getEnabledFilter(Member.StudentsOnlyFilter)).isDefined
		try {
			if (disableFilter)
				session.disableFilter(Member.StudentsOnlyFilter)

			val criteria = 
				session.newCriteria[Member]
					.add(is("userId", userId.safeTrim.toLowerCase))
					.add(isNull("missingFromImportSince"))
					.add(disjunction()
						.add(is("inUseFlag", "Active"))
						.add(like("inUseFlag", "Inactive - Starts %"))
					)
					.addOrder(asc("universityId"))
					
			if (eagerLoad)
				criteria
					.setFetchMode("studentCourseDetails", FetchMode.JOIN)
					.setFetchMode("studentCourseDetails.studentCourseYearDetails", FetchMode.JOIN)
					.setFetchMode("studentCourseDetails.moduleRegistrations", FetchMode.JOIN)
					.distinct
					
			criteria.seq
		} finally {
			if (disableFilter && filterEnabled)
				session.enableFilter(Member.StudentsOnlyFilter)
		}
	}

	def listUpdatedSince(startDate: DateTime, department: Department, max: Int) = {
		val homeDepartmentMatches = session.newCriteria[Member]
			.add(gt("lastUpdatedDate", startDate))
			.add(is("homeDepartment", department))
			.setMaxResults(max)
			.addOrder(asc("lastUpdatedDate"))
			.list

		val courseMatches = session.newQuery[StudentMember]( """
				select distinct student
        	from
          	StudentCourseDetails scd
          where
						scd.missingFromImportSince is null and
            scd.department = :department and
        		scd.student.lastUpdatedDate > :lastUpdated and
            scd.sprStatus.code not like 'P%' """)
			.setEntity("department", department)
			.setParameter("lastUpdated", startDate).seq

		// do not remove; import needed for sorting
		import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
		(homeDepartmentMatches ++ courseMatches).distinct.sortBy(_.lastUpdatedDate)
	}


	def listUpdatedSince(startDate: DateTime, max: Int) =
		session.newCriteria[Member].add(gt("lastUpdatedDate", startDate)).setMaxResults(max).addOrder(asc("lastUpdatedDate")).list

	def getAllCurrentRelationships(targetSprCode: String): Seq[StudentRelationship] = {
			session.newCriteria[StudentRelationship]
					.add(is("targetSprCode", targetSprCode))
					.add( Restrictions.or(
							Restrictions.isNull("endDate"),
							Restrictions.ge("endDate", new DateTime())
							))
					.seq
	}

	def getCurrentRelationships(relationshipType: StudentRelationshipType, targetSprCode: String): Seq[StudentRelationship] = {
			session.newCriteria[StudentRelationship]
					.add(is("targetSprCode", targetSprCode))
					.add(is("relationshipType", relationshipType))
					.add( Restrictions.or(
							Restrictions.isNull("endDate"),
							Restrictions.ge("endDate", new DateTime())
							))
					.seq
	}

	def getRelationshipsByTarget(relationshipType: StudentRelationshipType, targetSprCode: String): Seq[StudentRelationship] = {
			session.newCriteria[StudentRelationship]
					.add(is("targetSprCode", targetSprCode))
					.add(is("relationshipType", relationshipType))
					.seq
	}

	def getRelationshipsByDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentRelationship] = {
		// order by agent to separate any named (external) from numeric (member) agents
		// then by student properties
		session.newQuery[StudentRelationship]("""
			select
				distinct sr
			from
				StudentRelationship sr,
				StudentCourseDetails scd
			where
				sr.targetSprCode = scd.sprCode
			and
				sr.relationshipType = :relationshipType
			and
				scd.missingFromImportSince is null
			and
				scd.department = :department
			and
				scd.mostSignificant = true
			and
				scd.sprStatus.code not like 'P%'
			and
				(sr.endDate is null or sr.endDate >= SYSDATE)
			order by
				sr.agent, sr.targetSprCode
		""")
			.setEntity("department", department)
			.setEntity("relationshipType", relationshipType)
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
				sr.targetSprCode = scd.sprCode
      and
        staff.universityId = sr.agent
			and
				sr.relationshipType = :relationshipType
			and
				staff.homeDepartment = :department
			and
				scd.missingFromImportSince is null
			and
				scd.sprStatus.code not like 'P%'
			and
				(sr.endDate is null or sr.endDate >= SYSDATE)
			order by
				sr.agent, sr.targetSprCode
																					""")
			.setEntity("department", department)
			.setEntity("relationshipType", relationshipType)
			.seq
	}


	def getAllRelationshipsByAgent(agentId: String): Seq[StudentRelationship] =
		session.newCriteria[StudentRelationship]
			.add(is("agent", agentId))
			.add( Restrictions.or(
				Restrictions.isNull("endDate"),
				Restrictions.ge("endDate", new DateTime())
			))
			.seq


	def getAllRelationshipTypesByAgent(agentId: String): Seq[StudentRelationshipType] =
		session.newCriteria[StudentRelationship]
			.add(is("agent", agentId))
			.add( Restrictions.or(
				Restrictions.isNull("endDate"),
				Restrictions.ge("endDate", new DateTime())
			))
			.project[StudentRelationshipType](distinct(property("relationshipType")))
			.seq

	def getRelationshipsByAgent(relationshipType: StudentRelationshipType, agentId: String): Seq[StudentRelationship] =
		session.newCriteria[StudentRelationship]
			.add(is("agent", agentId))
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
				scd.missingFromImportSince is null
			and
				scd.department = :department
			and
				scd.mostSignificant = true
			and
				scd.sprStatus.code not like 'P%'
			and
				scd.sprCode not in (
					select
						sr.targetSprCode
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
	def getStudentsByDepartment(department: Department): Seq[StudentMember] =
		if (department == null) Nil
		else {

			val s = session.newQuery[StudentMember]("""
			select distinct student
			from
				StudentCourseDetails scd
			where
				scd.missingFromImportSince is null
			and
				scd.department = :department
			and
				scd.mostSignificant = true
			and
				scd.sprStatus.code not like 'P%'
			""")
			.setEntity("department", department).seq
			s
		}

	def getStaffByDepartment(department: Department): Seq[StaffMember] =
		if (department == null) Nil
		else {
			session.newCriteria[StaffMember]
				.add(is("homeDepartment", department))
				.seq
		}

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
				scd.missingFromImportSince is null
			and
				scd.department = :department
			and
				scd.mostSignificant = true
			and
				scd.sprStatus.code not like 'P%'
			and
				scd.sprCode in (select sr.targetSprCode from StudentRelationship sr where sr.relationshipType = :relationshipType)
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
				scd.missingFromImportSince is null
			and
				scd.sprCode in (select sr.targetSprCode from StudentRelationship sr where sr.relationshipType = :relationshipType)
			""")
			.setEntity("relationshipType", relationshipType)
			.uniqueResult.getOrElse(0)

	def findUniversityIdsByRestrictions(restrictions: Iterable[ScalaRestriction]): Seq[String] = {
		val idCriteria = session.newCriteria[StudentMember]
		idCriteria.add(isNull("missingFromImportSince"))
		restrictions.foreach { _.apply(idCriteria) }

		idCriteria.project[String](distinct(property("universityId"))).seq
	}

	def findStudentsByRestrictions(restrictions: Iterable[ScalaRestriction], orders: Iterable[ScalaOrder], maxResults: Int, startResult: Int): Seq[StudentMember] = {
		val universityIds = findUniversityIdsByRestrictions(restrictions)

		if (universityIds.isEmpty)
			return Seq()

		val c = session.newCriteria[StudentMember]

		val or = disjunction()
		universityIds.grouped(Daoisms.MaxInClauseCount).foreach { ids => or.add(in("universityId", ids)) }
		c.add(or)

		orders.foreach { c.addOrder(_) }

		c.setMaxResults(maxResults).setFirstResult(startResult).seq
	}

	def countStudentsByRestrictions(restrictions: Iterable[ScalaRestriction]) = {
		val c = session.newCriteria[StudentMember]
		c.add(isNull("missingFromImportSince"))
		restrictions.foreach { _.apply(c) }

		c.project[Number](countDistinct("universityId")).uniqueResult.get.intValue()
	}

	def getAllModesOfAttendance(department: Department) =
		session.newCriteria[StudentMember]
				.createAlias("mostSignificantCourse", "scd")
				.createAlias("scd.latestStudentCourseYearDetails", "scyd")
				.add(is("scd.department", department))
				.addOrder(desc("moaCount"))
				.project[Array[Any]](
					projectionList()
						.add(groupProperty("scyd.modeOfAttendance"))
						.add(rowCount(), "moaCount")
				)
				.seq.map { array => array(0).asInstanceOf[ModeOfAttendance] }

	def getAllSprStatuses(department: Department) =
		session.newCriteria[StudentMember]
				.createAlias("mostSignificantCourse", "scd")
				.add(is("scd.department", department))
				.addOrder(desc("statusCount"))
				.project[Array[Any]](
					projectionList()
						.add(groupProperty("scd.sprStatus"))
						.add(rowCount(), "statusCount")
				)
				.seq.map { array => array(0).asInstanceOf[SitsStatus] }

	def stampMissingFromImport(newStaleUniversityIds: Seq[String], importStart: DateTime) = {
		newStaleUniversityIds.grouped(Daoisms.MaxInClauseCount).foreach { staleIds =>
			val sqlString = """
				update
					Member
				set
					missingFromImportSince = :importStart
				where
					universityId in (:newStaleUniversityIds)
				"""

				session.newQuery(sqlString)
					.setParameter("importStart", importStart)
					.setParameterList("newStaleUniversityIds", staleIds)
					.executeUpdate
			}
		}
}

