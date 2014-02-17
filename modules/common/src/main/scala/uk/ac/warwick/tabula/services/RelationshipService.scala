package uk.ac.warwick.tabula.services

import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Department, Member, StudentMember, StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

/**
 * Service providing access to members and profiles.
 */
trait RelationshipService {
	def getPreviousRelationship(relationship: StudentRelationship): Option[StudentRelationship]

	def allStudentRelationshipTypes: Seq[StudentRelationshipType]
	def saveOrUpdate(relationshipType: StudentRelationshipType)
	def delete(relationshipType: StudentRelationshipType)
	def getStudentRelationshipTypeById(id: String): Option[StudentRelationshipType]
	def getStudentRelationshipTypeByUrlPart(urlPart: String): Option[StudentRelationshipType]
	def getStudentRelationshipTypesWithRdxType: Seq[StudentRelationshipType]

	def saveOrUpdate(relationship: StudentRelationship)
	def findCurrentRelationships(relationshipType: StudentRelationshipType, student: StudentMember): Seq[StudentRelationship]
	def getRelationships(relationshipType: StudentRelationshipType, student: StudentMember): Seq[StudentRelationship]
	def saveStudentRelationships(relationshipType: StudentRelationshipType, studentCourseDetails: StudentCourseDetails, agents: Seq[Member]): Seq[StudentRelationship]
	def replaceStudentRelationships(relationshipType: StudentRelationshipType, studentCourseDetails: StudentCourseDetails, agents: Seq[Member]): Seq[StudentRelationship]
	def replaceStudentRelationshipsWithPercentages(relationshipType: StudentRelationshipType, studentCourseDetails: StudentCourseDetails, agentsWithPercentages: Seq[(Member, JBigDecimal)]): Seq[StudentRelationship]
	def listStudentRelationshipsByDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentRelationship]
	def listStudentRelationshipsByStaffDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentRelationship]
	def listAllStudentRelationshipsWithMember(agent: Member): Seq[StudentRelationship]
	def listAllStudentRelationshipTypesWithMember(agent: Member): Seq[StudentRelationshipType]
	def listStudentRelationshipsWithMember(relationshipType: StudentRelationshipType, agent: Member): Seq[StudentRelationship]
	def listAllStudentRelationshipsWithUniversityId(agentId: String): Seq[StudentRelationship]
	def listStudentRelationshipsWithUniversityId(relationshipType: StudentRelationshipType, agentId: String): Seq[StudentRelationship]
	def listStudentsWithoutRelationship(relationshipType: StudentRelationshipType, department: Department): Seq[Member]
	def countStudentsByRelationshipAndDepartment(relationshipType: StudentRelationshipType, department: Department): (Int, Int)
	def countStudentsByRelationship(relationshipType: StudentRelationshipType): Int
	def getAllCurrentRelationships(student: StudentMember): Seq[StudentRelationship]
}

@Service(value = "relationshipService")
class RelationshipServiceImpl extends RelationshipService with Logging {

	var memberDao = Wire.auto[MemberDao]
	var profileService = Wire.auto[ProfileService]
	var profileIndexService = Wire.auto[ProfileIndexService]

	def allStudentRelationshipTypes: Seq[StudentRelationshipType] = memberDao.allStudentRelationshipTypes
	def getStudentRelationshipTypeById(id: String) = memberDao.getStudentRelationshipTypeById(id)

	def getStudentRelationshipTypeByUrlPart(urlPart: String) = memberDao.getStudentRelationshipTypeByUrlPart(urlPart)

	def saveOrUpdate(relationshipType: StudentRelationshipType) = memberDao.saveOrUpdate(relationshipType)
	def delete(relationshipType: StudentRelationshipType) = memberDao.delete(relationshipType)

	def saveOrUpdate(relationship: StudentRelationship) = memberDao.saveOrUpdate(relationship)

	def findCurrentRelationships(relationshipType: StudentRelationshipType, student: StudentMember): Seq[StudentRelationship] = transactional() {
		memberDao.getCurrentRelationships(relationshipType, student)
	}

	def getAllCurrentRelationships(student: StudentMember): Seq[StudentRelationship] = transactional(readOnly = true) {
		memberDao.getAllCurrentRelationships(student)
	}

	def getRelationships(relationshipType: StudentRelationshipType, student: StudentMember): Seq[StudentRelationship] = transactional(readOnly = true) {
		memberDao.getRelationshipsByTarget(relationshipType, student)
	}

	def saveStudentRelationships(relationshipType: StudentRelationshipType, studentCourseDetails: StudentCourseDetails, agents: Seq[Member]): Seq[StudentRelationship] = transactional() {
		val currentRelationships = findCurrentRelationships(relationshipType, studentCourseDetails.student)
		val existingRelationships = currentRelationships.filter { rel => rel.agentMember.exists { agents.contains(_) } }
		val agentsToCreate = agents.filterNot { agent => currentRelationships.exists { _.agentMember == Some(agent) } }

		agentsToCreate.map { agent =>
			// create the new one
			val newRelationship = StudentRelationship(agent, relationshipType, studentCourseDetails)
			newRelationship.startDate = new DateTime
			memberDao.saveOrUpdate(newRelationship)
			newRelationship
		} ++ existingRelationships
	}

	def saveStudentRelationshipsWithPercentages(relationshipType: StudentRelationshipType, studentCourseDetails: StudentCourseDetails, agents: Seq[(Member, JBigDecimal)]): Seq[StudentRelationship] = transactional() {
		val currentRelationships = findCurrentRelationships(relationshipType, studentCourseDetails.student)
		val existingRelationships = currentRelationships.filter { rel => rel.agentMember.exists { agent => agents.map { _._1 }.contains(agent) } }
		val agentsToCreate = agents.filterNot { case (agent, _) => currentRelationships.exists { _.agentMember == Some(agent) } }

		agentsToCreate.map { case (agent, percentage) =>
			// create the new one
			val newRelationship = StudentRelationship(agent, relationshipType, studentCourseDetails)
			newRelationship.percentage = percentage
			newRelationship.startDate = new DateTime
			memberDao.saveOrUpdate(newRelationship)
			newRelationship
		} ++ existingRelationships
	}

	// end any existing relationships of the same type for this student, then save the new one
	def replaceStudentRelationships(relationshipType: StudentRelationshipType, studentCourseDetails: StudentCourseDetails, agents: Seq[Member]): Seq[StudentRelationship] = transactional() {
		val currentRelationships = findCurrentRelationships(relationshipType, studentCourseDetails.student)
		val (existingRelationships, relationshipsToEnd) = currentRelationships.partition { rel => rel.agentMember.exists { agents.contains(_) } }

		val agentsToAdd = agents.filterNot { agent => existingRelationships.exists { _.agentMember == Some(agent) } }

		// Don't need to do anything with existingRelationships, but need to handle the others

		// End all relationships for agents not passed in
		relationshipsToEnd.foreach { _.endDate = DateTime.now }

		// Save new relationships for agents that don't already exist
		saveStudentRelationships(relationshipType, studentCourseDetails, agentsToAdd)
	}

	def replaceStudentRelationshipsWithPercentages(relationshipType: StudentRelationshipType, studentCourseDetails: StudentCourseDetails, agents: Seq[(Member, JBigDecimal)]): Seq[StudentRelationship] = transactional() {
		val currentRelationships = findCurrentRelationships(relationshipType, studentCourseDetails.student)
		val (existingRelationships, relationshipsToEnd) = currentRelationships.partition { rel => rel.agentMember.exists { agent => agents.map { _._1 }.contains(agent) } }

		val agentsToAdd = agents.filterNot { case (agent, percentage) => existingRelationships.exists { _.agentMember == Some(agent) } }

		// Find existing relationships with the wrong percentage
		existingRelationships.foreach { rel =>
			val percentage = agents.find { case (agent, _) => rel.agentMember.exists { agent == _ } }.get._2
			if (rel.percentage != percentage) {
				rel.percentage = percentage
				memberDao.saveOrUpdate(rel)
			}
		}

		// Don't need to do anything with existingRelationships, but need to handle the others

		// End all relationships for agents not passed in
		relationshipsToEnd.foreach { _.endDate = DateTime.now }

		// Save new relationships for agents that don't already exist
		saveStudentRelationshipsWithPercentages(relationshipType, studentCourseDetails, agentsToAdd)
	}

	def relationshipDepartmentFilterMatches(department: Department)(rel: StudentRelationship) =
		rel.studentMember.exists(studentDepartmentFilterMatches(department))

	def relationshipNotPermanentlyWithdrawn(rel: StudentRelationship): Boolean = {
		Option(rel.studentCourseDetails).exists(scd => !scd.permanentlyWithdrawn)
	}

	def studentDepartmentFilterMatches(department: Department)(member: StudentMember)	= department.filterRule.matches(member)

	def studentNotPermanentlyWithdrawn(member: StudentMember) = !member.permanentlyWithdrawn

	def expectedToHaveRelationship(relationshipType: StudentRelationshipType, department: Department)(member: StudentMember) = {
		member.freshStudentCourseDetails
		.filter(scd => Option(scd.route).exists(_.department == department)) // there needs to be an SCD for the right department ...
		.filter(!_.permanentlyWithdrawn) // that's not permanently withdrawn ...
		.filter(relationshipType.isExpected) // and has a course of the type that is expected to have this kind of relationship
		.nonEmpty
	}

	def listStudentRelationshipsByDepartment(relationshipType: StudentRelationshipType, department: Department) = transactional(readOnly = true) {
		memberDao.getRelationshipsByDepartment(relationshipType, department.rootDepartment)
			.filter(relationshipDepartmentFilterMatches(department))
			.filter(relationshipNotPermanentlyWithdrawn)
	}

	def listStudentRelationshipsByStaffDepartment(relationshipType: StudentRelationshipType, department: Department) = transactional(readOnly = true) {
		memberDao.getRelationshipsByStaffDepartment(relationshipType, department.rootDepartment)
			.filter(relationshipDepartmentFilterMatches(department))
			.filter(relationshipNotPermanentlyWithdrawn)
	}

	def listAllStudentRelationshipsWithMember(agent: Member) = transactional(readOnly = true) {
		memberDao.getAllRelationshipsByAgent(agent.universityId)
			.filter(relationshipNotPermanentlyWithdrawn)
	}

	def listAllStudentRelationshipTypesWithMember(agent: Member) = transactional(readOnly = true) {
		memberDao.getAllRelationshipTypesByAgent(agent.universityId)
	}

	def listStudentRelationshipsWithMember(relationshipType: StudentRelationshipType, agent: Member) = transactional(readOnly = true) {
		memberDao.getRelationshipsByAgent(relationshipType, agent.universityId)
			.filter(relationshipNotPermanentlyWithdrawn)
	}

	def listAllStudentRelationshipsWithUniversityId(agentId: String) = transactional(readOnly = true) {
		memberDao.getAllRelationshipsByAgent(agentId)
			.filter(relationshipNotPermanentlyWithdrawn)
	}

	def listStudentRelationshipsWithUniversityId(relationshipType: StudentRelationshipType, agentId: String) = transactional(readOnly = true) {
		memberDao.getRelationshipsByAgent(relationshipType, agentId)
			.filter(relationshipNotPermanentlyWithdrawn)
	}

  def listStudentsWithoutRelationship(relationshipType: StudentRelationshipType, department: Department) = transactional(readOnly = true) {
		memberDao.getStudentsWithoutRelationshipByDepartment(relationshipType, department.rootDepartment)
			.filter(studentDepartmentFilterMatches(department))
			.filter(expectedToHaveRelationship(relationshipType, department))
  }

  def countStudentsByRelationshipAndDepartment(relationshipType: StudentRelationshipType, department: Department): (Int, Int) = transactional(readOnly = true) {
		val matchingStudents =
			memberDao.getStudentsByRelationshipAndDepartment(relationshipType, department.rootDepartment)
				.filter(studentDepartmentFilterMatches(department))
				.filter(studentNotPermanentlyWithdrawn)
		(profileService.countStudentsByDepartment(department), matchingStudents.size)
	}

  def countStudentsByRelationship(relationshipType: StudentRelationshipType): Int = transactional(readOnly = true) {
		memberDao.countStudentsByRelationship(relationshipType).intValue
	}

	def getStudentRelationshipTypesWithRdxType: Seq[StudentRelationshipType] = {
		allStudentRelationshipTypes.filter(_.defaultRdxType != null)
	}

	def getPreviousRelationship(relationship: StudentRelationship): Option[StudentRelationship] = {
		relationship.studentMember.flatMap { student =>
			val rels = getRelationships(relationship.relationshipType, student)
			val sortedRels = rels.sortBy { _.startDate }

			// Get the element before the current relationship
			val index = sortedRels.indexOf(relationship)
			if (index > 0) {
				Some(sortedRels(index-1))
			} else {
				None
			}
		}
	}
}

trait RelationshipServiceComponent {
	var relationshipService: RelationshipService
}
trait AutowiringRelationshipServiceComponent extends RelationshipServiceComponent{
	var relationshipService = Wire[RelationshipService]
}
