package uk.ac.warwick.tabula.services

import org.hibernate.criterion.Restrictions
import org.hibernate.sql.JoinType
import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{FiltersStudents, StudentAssociationData, StudentAssociationEntityData, TaskBenchmarking}
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.JavaImports._

import scala.collection.immutable.TreeMap

trait RelationshipServiceComponent {
	def relationshipService: RelationshipService
}

trait AutowiringRelationshipServiceComponent extends RelationshipServiceComponent {
	var relationshipService: RelationshipService = Wire[RelationshipService]
}

case class SortableAgentIdentifier(agentId: String, lastName: Option[String]){
	val sortkey: String = lastName.getOrElse("") + agentId
	override def toString: String = sortkey
}
object SortableAgentIdentifier{
	def apply(r:StudentRelationship): SortableAgentIdentifier = SortableAgentIdentifier(r.agent, r.agentMember.map(_.lastName))

	val KeyOrdering: Ordering[SortableAgentIdentifier] = Ordering.by { a:SortableAgentIdentifier => a.sortkey }
}

/**
 * Service providing access to members and profiles.
 */
trait RelationshipService {
	def allStudentRelationshipTypes: Seq[StudentRelationshipType]
	def saveOrUpdate(relationshipType: StudentRelationshipType)
	def delete(relationshipType: StudentRelationshipType)
	def getStudentRelationshipTypeById(id: String): Option[StudentRelationshipType]
	def getStudentRelationshipTypeByUrlPart(urlPart: String): Option[StudentRelationshipType]
	def getStudentRelationshipTypesWithRdxType: Seq[StudentRelationshipType]
	def getStudentRelationshipById(id: String): Option[StudentRelationship]

	def saveOrUpdate(relationship: StudentRelationship)
	def findCurrentRelationships(relationshipType: StudentRelationshipType, scd: StudentCourseDetails): Seq[StudentRelationship]
	def findCurrentRelationships(relationshipType: StudentRelationshipType, student: StudentMember): Seq[StudentRelationship]
	def getCurrentRelationship(relationshipType: StudentRelationshipType, student: StudentMember, agent: Member): Option[StudentRelationship]
	def getCurrentRelationships(student: StudentMember, agentId: String): Seq[StudentRelationship]
	def getRelationships(relationshipType: StudentRelationshipType, studentCourseDetails:StudentCourseDetails): Seq[StudentRelationship]
	def getRelationships(relationshipType: StudentRelationshipType, student: StudentMember): Seq[StudentRelationship]
	def saveStudentRelationships(
		relationshipType: StudentRelationshipType,
		studentCourseDetails: StudentCourseDetails,
		agents: Seq[Member]
	): Seq[StudentRelationship]
	def replaceStudentRelationships(
		relationshipType: StudentRelationshipType,
		studentCourseDetails: StudentCourseDetails,
		agents: Seq[Member]
	): Seq[StudentRelationship]
	def replaceStudentRelationshipsWithPercentages(
		relationshipType: StudentRelationshipType,
		studentCourseDetails: StudentCourseDetails,
		agentsWithPercentages: Seq[(Member, JBigDecimal)]
	): Seq[StudentRelationship]
	def listStudentRelationshipsByDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentRelationship]
	def listStudentRelationshipsByStaffDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentRelationship]
	def listAllStudentRelationshipsWithMember(agent: Member): Seq[StudentRelationship]
	def listAllStudentRelationshipTypesWithStudentMember(student: StudentMember): Seq[StudentRelationshipType]
	def listAllStudentRelationshipTypesWithMember(agent: Member): Seq[StudentRelationshipType]
	def listStudentRelationshipsWithMember(relationshipType: StudentRelationshipType, agent: Member): Seq[StudentRelationship]
	def listStudentRelationshipsWithMemberInDepartment(relationshipType: StudentRelationshipType, agent: Member, department: Department): Seq[StudentRelationship]
	def listAllStudentRelationshipsWithUniversityId(agentId: String): Seq[StudentRelationship]
	def listStudentRelationshipsWithUniversityId(relationshipType: StudentRelationshipType, agentId: String): Seq[StudentRelationship]
	def listStudentsWithoutRelationship(relationshipType: StudentRelationshipType, department: Department): Seq[Member]
	def countStudentsByRelationship(relationshipType: StudentRelationshipType): Int
	def getAllCurrentRelationships(student: StudentMember): Seq[StudentRelationship]
	def getAllPastAndPresentRelationships(student: StudentMember): Seq[StudentRelationship]
	def getAllPastAndPresentRelationships(relationshipType: StudentRelationshipType, scd: StudentCourseDetails): Seq[StudentRelationship]
	def endStudentRelationships(relationships: Seq[StudentRelationship])
	def getStudentAssociationDataWithoutRelationship(department: Department, relationshipType: StudentRelationshipType, restrictions: Seq[ScalaRestriction] = Seq()): Seq[StudentAssociationData]
	def getStudentAssociationEntityData(department: Department, relationshipType: StudentRelationshipType, additionalEntityIds: Seq[String]): Seq[StudentAssociationEntityData]
	def listCurrentRelationshipsWithAgent(relationshipType: StudentRelationshipType, agentId: String): Seq[StudentRelationship]
	def applyStudentRelationships(relationshipType: StudentRelationshipType, agentId: String, studentIDs: Seq[String]): Seq[StudentRelationship]
	def coursesForStudentCourseDetails(scds: Seq[StudentCourseDetails]): Map[StudentCourseDetails, Course]
	def latestYearsOfStudyForStudentCourseDetails(scds: Seq[StudentCourseDetails]): Map[StudentCourseDetails, Int]
	def listAgentRelationshipsByDepartment(relationshipType: StudentRelationshipType, department: Department): TreeMap[SortableAgentIdentifier, Seq[StudentRelationship]]
}

abstract class AbstractRelationshipService extends RelationshipService with Logging with TaskBenchmarking {

	self: RelationshipDaoComponent with ProfileServiceComponent =>

	def saveOrUpdate(relationship: StudentRelationship): Unit = relationshipDao.saveOrUpdate(relationship)

	def allStudentRelationshipTypes: Seq[StudentRelationshipType] = relationshipDao.allStudentRelationshipTypes
	def getStudentRelationshipTypeById(id: String): Option[StudentRelationshipType] = relationshipDao.getStudentRelationshipTypeById(id)

	def getStudentRelationshipTypeByUrlPart(urlPart: String): Option[StudentRelationshipType] = relationshipDao.getStudentRelationshipTypeByUrlPart(urlPart)

	def saveOrUpdate(relationshipType: StudentRelationshipType): Unit = relationshipDao.saveOrUpdate(relationshipType)
	def delete(relationshipType: StudentRelationshipType): Unit = relationshipDao.delete(relationshipType)

	def findCurrentRelationships(relationshipType: StudentRelationshipType, scd: StudentCourseDetails): Seq[StudentRelationship] = transactional(){
		relationshipDao.getCurrentRelationships(relationshipType, scd)
	}

	def findCurrentRelationships(relationshipType: StudentRelationshipType, student: StudentMember): Seq[StudentRelationship] = transactional() {
		relationshipDao.getCurrentRelationships(relationshipType, student)
	}

	def getCurrentRelationship(relationshipType: StudentRelationshipType, student: StudentMember, agent: Member): Option[StudentRelationship] = transactional() {
		relationshipDao.getCurrentRelationship(relationshipType, student, agent)
	}

	def getCurrentRelationships(student: StudentMember, agentId: String): Seq[StudentRelationship] = transactional() {
		relationshipDao.getCurrentRelationships(student, agentId).filter(relationshipNotPermanentlyWithdrawn)
	}

	def getAllCurrentRelationships(student: StudentMember): Seq[StudentRelationship] = transactional(readOnly = true) {
		relationshipDao.getAllCurrentRelationships(student)
	}

	def getAllPastAndPresentRelationships(student: StudentMember): Seq[StudentRelationship] = transactional(readOnly = true) {
		relationshipDao.getAllPastAndPresentRelationships(student)
	}

	def getAllPastAndPresentRelationships(relationshipType: StudentRelationshipType, scd: StudentCourseDetails): Seq[StudentRelationship] = transactional(readOnly = true) {
		relationshipDao.getAllPastAndPresentRelationships(relationshipType, scd)
	}

	def getRelationships(relationshipType: StudentRelationshipType, studentCourseDetails:StudentCourseDetails): Seq[StudentRelationship] = transactional(readOnly = true) {
		relationshipDao.getRelationshipsByCourseDetails(relationshipType, studentCourseDetails)
	}

	def getRelationships(relationshipType: StudentRelationshipType, student: StudentMember): Seq[StudentRelationship] = transactional(readOnly = true) {
		relationshipDao.getRelationshipsByTarget(relationshipType, student)
	}

	def saveStudentRelationships(
		relationshipType: StudentRelationshipType,
		studentCourseDetails: StudentCourseDetails,
		agents: Seq[Member]
	): Seq[StudentRelationship] = transactional() {
		val currentRelationships = findCurrentRelationships(relationshipType, studentCourseDetails)
		val existingRelationships = currentRelationships.filter { rel => rel.agentMember.exists { agents.contains(_) } }
		val agentsToCreate = agents.filterNot { agent => currentRelationships.exists(_.agentMember.contains(agent)) }

		agentsToCreate.map { agent =>
			// create the new one
			val newRelationship = StudentRelationship(agent, relationshipType, studentCourseDetails)
			newRelationship.startDate = new DateTime
			relationshipDao.saveOrUpdate(newRelationship)
			newRelationship
		} ++ existingRelationships
	}

	def saveStudentRelationshipsWithPercentages(
		relationshipType: StudentRelationshipType,
		studentCourseDetails: StudentCourseDetails,
		agents: Seq[(Member, JBigDecimal)]
	): Seq[StudentRelationship] = transactional() {
		val currentRelationships = findCurrentRelationships(relationshipType, studentCourseDetails)
		val existingRelationships = currentRelationships.filter { rel => rel.agentMember.exists { agent => agents.map { _._1 }.contains(agent) } }
		val agentsToCreate = agents.filterNot { case (agent, _) => currentRelationships.exists(_.agentMember.contains(agent)) }

		agentsToCreate.map { case (agent, percentage) =>
			// create the new one
			val newRelationship = StudentRelationship(agent, relationshipType, studentCourseDetails)
			newRelationship.percentage = percentage
			newRelationship.startDate = new DateTime
			relationshipDao.saveOrUpdate(newRelationship)
			newRelationship
		} ++ existingRelationships
	}

	// end any existing relationships of the same type for this student, then save the new one
	def replaceStudentRelationships(
		relationshipType: StudentRelationshipType,
		studentCourseDetails: StudentCourseDetails,
		agents: Seq[Member]
	): Seq[StudentRelationship] = transactional() {
		val currentRelationships = findCurrentRelationships(relationshipType, studentCourseDetails)
		val (existingRelationships, relationshipsToEnd) = currentRelationships.partition { rel => rel.agentMember.exists { agents.contains(_) } }

		val agentsToAdd = agents.filterNot { agent => existingRelationships.exists(_.agentMember.contains(agent)) }

		// Don't need to do anything with existingRelationships, but need to handle the others

		// End all relationships for agents not passed in
		endStudentRelationships(relationshipsToEnd)

		// Save new relationships for agents that don't already exist
		saveStudentRelationships(relationshipType, studentCourseDetails, agentsToAdd)
	}

	def endStudentRelationships(relationships: Seq[StudentRelationship]) {
		relationships.foreach {
			rel => {
				rel.endDate = DateTime.now
				saveOrUpdate(rel)
			}
		}
	}

	def replaceStudentRelationshipsWithPercentages(
		relationshipType: StudentRelationshipType,
		studentCourseDetails: StudentCourseDetails,
		agents: Seq[(Member, JBigDecimal)]
	): Seq[StudentRelationship] = transactional() {
		val currentRelationships = findCurrentRelationships(relationshipType, studentCourseDetails)
		val (existingRelationships, relationshipsToEnd) = currentRelationships.partition {
			rel => rel.agentMember.exists { agent => agents.map { _._1 }.contains(agent) }
		}

		val agentsToAdd = agents.filterNot { case (agent, percentage) => existingRelationships.exists(_.agentMember.contains(agent)) }

		// Find existing relationships with the wrong percentage
		existingRelationships.foreach { rel =>
			val percentage = agents.find { case (agent, _) => rel.agentMember.contains(agent) }.get._2
			if (rel.percentage != percentage) {
				rel.percentage = percentage
				relationshipDao.saveOrUpdate(rel)
			}
		}

		// Don't need to do anything with existingRelationships, but need to handle the others

		// End all relationships for agents not passed in
		relationshipsToEnd.foreach { _.endDate = DateTime.now }

		// Save new relationships for agents that don't already exist
		saveStudentRelationshipsWithPercentages(relationshipType, studentCourseDetails, agentsToAdd)
	}

	def relationshipDepartmentFilterMatches(department: Department)(rel: StudentRelationship): Boolean =
		rel.studentMember.exists(studentDepartmentFilterMatches(department))

	def relationshipNotPermanentlyWithdrawn(rel: StudentRelationship): Boolean = {
		Option(rel.studentCourseDetails).exists(
			scd => !scd.permanentlyWithdrawn && scd.missingFromImportSince == null)
	}

	def studentDepartmentFilterMatches(department: Department)(member: StudentMember): Boolean = department.filterRule.matches(member, Option(department))

	def studentNotPermanentlyWithdrawn(member: StudentMember): Boolean = !member.permanentlyWithdrawn

	def studentDepartmentMatchesAndExpectedToHaveRelationship(relationshipType: StudentRelationshipType, department: Department)(member: StudentMember): Boolean = {
		department.filterRule.matches(member, Option(department)) &&
		member.freshStudentCourseDetails
		.filter(scd => Option(scd.currentRoute).exists(route => route.adminDepartment == department || route.adminDepartment == department.rootDepartment)) // there needs to be an SCD for the right department ...
		.filter(!_.permanentlyWithdrawn) // that's not permanently withdrawn ...
		.exists(relationshipType.isExpected) // and has a course of the type that is expected to have this kind of relationship
	}

	def listStudentRelationshipsByDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentRelationship] = transactional(readOnly = true) {
		benchmarkTask("listStudentRelationshipsByDepartment") {
		relationshipDao.getRelationshipsByDepartment(relationshipType, department.rootDepartment)
			.filter(relationshipDepartmentFilterMatches(department))
			.filter(relationshipNotPermanentlyWithdrawn)
	}}

	def listStudentRelationshipsByStaffDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentRelationship] = transactional(readOnly = true) {
		relationshipDao.getRelationshipsByStaffDepartment(relationshipType, department.rootDepartment)
			.filter(relationshipDepartmentFilterMatches(department))
			.filter(relationshipNotPermanentlyWithdrawn)
	}

	def listAllStudentRelationshipsWithMember(agent: Member): Seq[MemberStudentRelationship] = transactional(readOnly = true) {
		relationshipDao.getAllRelationshipsByAgent(agent.universityId)
			.filter(relationshipNotPermanentlyWithdrawn)
	}

	def listAllStudentRelationshipTypesWithStudentMember(student: StudentMember): Seq[StudentRelationshipType] = transactional(readOnly = true) {
		relationshipDao.getAllRelationshipTypesByStudent(student)
	}


	def listAllStudentRelationshipTypesWithMember(agent: Member): Seq[StudentRelationshipType] = transactional(readOnly = true) {
		relationshipDao.getAllRelationshipTypesByAgent(agent.universityId)
	}

	def listStudentRelationshipsWithMember(relationshipType: StudentRelationshipType, agent: Member): Seq[MemberStudentRelationship] = transactional(readOnly = true) {
		relationshipDao.getRelationshipsByAgent(relationshipType, agent.universityId)
			.filter(relationshipNotPermanentlyWithdrawn)
	}

	def listStudentRelationshipsWithMemberInDepartment(relationshipType: StudentRelationshipType, agent: Member, department: Department): Seq[MemberStudentRelationship] = transactional(readOnly = true) {
		relationshipDao.getRelationshipsByAgent(relationshipType, agent.universityId)
			.filter(relationshipNotPermanentlyWithdrawn)
			.filter(r => r.studentCourseDetails.department == department.rootDepartment)
	}

	def listAllStudentRelationshipsWithUniversityId(agentId: String): Seq[MemberStudentRelationship] = transactional(readOnly = true) {
		relationshipDao.getAllRelationshipsByAgent(agentId)
			.filter(relationshipNotPermanentlyWithdrawn)
	}

	def listStudentRelationshipsWithUniversityId(relationshipType: StudentRelationshipType, agentId: String): Seq[MemberStudentRelationship] = transactional(readOnly = true) {
		relationshipDao.getRelationshipsByAgent(relationshipType, agentId)
			.filter(relationshipNotPermanentlyWithdrawn)
	}

  def listStudentsWithoutRelationship(relationshipType: StudentRelationshipType, department: Department): Seq[StudentMember] = transactional(readOnly = true) {
		benchmarkTask("listStudentsWithoutRelationship") {
			relationshipDao.getStudentsWithoutRelationshipByDepartment(relationshipType, department.rootDepartment)
				.filter(studentDepartmentMatchesAndExpectedToHaveRelationship(relationshipType, department))
		}
  }

  def countStudentsByRelationship(relationshipType: StudentRelationshipType): Int = transactional(readOnly = true) {
		relationshipDao.countStudentsByRelationship(relationshipType).intValue
	}

	def getStudentRelationshipTypesWithRdxType: Seq[StudentRelationshipType] = {
		allStudentRelationshipTypes.filter(_.defaultRdxType != null)
	}

	def getStudentRelationshipById(id: String): Option[StudentRelationship] = relationshipDao.getStudentRelationshipById(id)

	/**
	 * Students enrolled in the department and matching the department filter
	 */
	private def departmentRestrictions(department: Department): Iterable[ScalaRestriction] = {
		ScalaRestriction.is(
			"mostSignificantCourse.department", department.rootDepartment,
			FiltersStudents.AliasPaths("mostSignificantCourse"): _*
		) ++ department.filterRule.restriction(FiltersStudents.AliasPaths)
	}

	private val notPermanentlyWithdrawnRestriction = ScalaRestriction.custom(
		Restrictions.not(Restrictions.like("mostSignificantCourse.statusOnRoute.code", "P%")),
		FiltersStudents.AliasPaths("statusOnRoute"): _*
	)

	def getStudentAssociationDataWithoutRelationship(department: Department, relationshipType: StudentRelationshipType, restrictions: Seq[ScalaRestriction] = Seq()): Seq[StudentAssociationData] = transactional(readOnly = true) {
		benchmarkTask("getStudentAssociationDataWithoutRelationship") {
			val allRestrictions = departmentRestrictions(department) ++ notPermanentlyWithdrawnRestriction ++
				// For this relationship type and not expired, but null
				ScalaRestriction.custom(
					Restrictions.isNull("relationshipsOfType.id"),
					"mostSignificantCourse" -> AliasAndJoinType("mostSignificantCourse"),
					"mostSignificantCourse.allRelationships" ->
						AliasAndJoinType("relationshipsOfType", JoinType.LEFT_OUTER_JOIN, Some(Restrictions.and(
							Restrictions.eq("relationshipType", relationshipType),
							Restrictions.or(
								Restrictions.isNull("endDate"),
								Restrictions.gt("endDate", DateTime.now)
							)
						)))
				) ++
				// Plus whatever was passed in
				restrictions

			relationshipDao.getStudentAssociationData(allRestrictions)
				// Only return students who are expected to have this type of relationship
				.filter(student => relationshipType.displayIfEmpty(student.courseType, department))
		}
	}

	def getStudentAssociationEntityData(department: Department, relationshipType: StudentRelationshipType, additionalEntityIds: Seq[String]): Seq[StudentAssociationEntityData] = transactional(readOnly = true) {
		benchmarkTask("getStudentAssociationEntityData") {
			val studentData = relationshipDao.getStudentAssociationData(departmentRestrictions(department) ++ notPermanentlyWithdrawnRestriction)
			relationshipDao.getStudentAssociationEntityData(department, relationshipType, studentData, additionalEntityIds)
		}
	}

	def listCurrentRelationshipsWithAgent(relationshipType: StudentRelationshipType, agentId: String): Seq[StudentRelationship] = {
		benchmarkTask("listCurrentRelationshipsWithAgent") {
			relationshipDao.listCurrentRelationshipsWithAgent(relationshipType, agentId)
		}
	}

	def applyStudentRelationships(relationshipType: StudentRelationshipType, agentId: String, studentIDs: Seq[String]): Seq[StudentRelationship] = {
		val allStudents = profileService.getAllMembersWithUniversityIdsStaleOrFresh(studentIDs).flatMap{
			case student: StudentMember => Some(student)
			case _ => None
		}
		val relationships = profileService.getMemberByUniversityIdStaleOrFresh(agentId) match {
			case Some(agentMember: Member) => allStudents.map(s => StudentRelationship.apply(agentMember, relationshipType, s))
			case None => allStudents.map(s => ExternalStudentRelationship.apply(agentId, relationshipType, s))
		}
		relationships.foreach(saveOrUpdate)
		relationships
	}

	def coursesForStudentCourseDetails(scds: Seq[StudentCourseDetails]): Map[StudentCourseDetails, Course] = {
		relationshipDao.coursesForStudentCourseDetails(scds)
	}

	def latestYearsOfStudyForStudentCourseDetails(scds: Seq[StudentCourseDetails]): Map[StudentCourseDetails, Int] = {
		relationshipDao.latestYearsOfStudyForStudentCourseDetails(scds)
	}

	def listAgentRelationshipsByDepartment(relationshipType: StudentRelationshipType, department: Department): TreeMap[SortableAgentIdentifier, Seq[StudentRelationship]] = {
		// all students in department X
		val unsortedAgentRelationshipsByStudentDept = listStudentRelationshipsByDepartment(relationshipType, department)

		// all students with a tutor in department X
		val unsortedAgentRelationshipsByStaffDept = listStudentRelationshipsByStaffDepartment(relationshipType, department)

		// combine the two and remove the dups
		val unsortedAgentRelationships = (unsortedAgentRelationshipsByStudentDept ++ unsortedAgentRelationshipsByStaffDept)
			// TAB-2750 treat relationships between the same agent and student COURSE as identical
			.groupBy { rel => (rel.agent, rel.studentCourseDetails) }
			.map { case (_, rels) => rels.maxBy { rel => rel.startDate.getMillis } }
			.toSeq

		// group into map by agent lastname, or id if the lastname is unavailable
		val groupedAgentRelationships = unsortedAgentRelationships.groupBy(r=>SortableAgentIdentifier(r))

		// alpha sort by constructing a TreeMap
		TreeMap(groupedAgentRelationships.toSeq:_*)(SortableAgentIdentifier.KeyOrdering)
	}
}

@Service("relationshipService")
class RelationshipServiceImpl
	extends AbstractRelationshipService
	with AutowiringRelationshipDaoComponent
	with AutowiringProfileServiceComponent
