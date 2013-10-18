package uk.ac.warwick.tabula.services

import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.helpers.Logging
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

/**
 * Service providing access to members and profiles.
 */
trait RelationshipService {
	def allStudentRelationshipTypes: Seq[StudentRelationshipType]
	def saveOrUpdate(relationshipType: StudentRelationshipType)
	def delete(relationshipType: StudentRelationshipType)
	def getStudentRelationshipTypeById(id: String): Option[StudentRelationshipType]
	def getStudentRelationshipTypeByUrlPart(urlPart: String): Option[StudentRelationshipType]
	
	def saveOrUpdate(relationship: StudentRelationship)
	def findCurrentRelationships(relationshipType: StudentRelationshipType, targetSprCode: String): Seq[StudentRelationship]
	def getRelationships(relationshipType: StudentRelationshipType, targetUniversityId: String): Seq[StudentRelationship]
	def saveStudentRelationship(relationshipType: StudentRelationshipType, targetSprCode: String, agent: String): StudentRelationship
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
		student.studentCourseDetails.asScala.flatMap {
			courseDetail => memberDao.getCurrentRelationships(relationshipType, courseDetail.sprCode)
		}
	}

	def findCurrentRelationships(relationshipType: StudentRelationshipType, targetSprCode: String): Seq[StudentRelationship] = transactional() {
		memberDao.getCurrentRelationships(relationshipType, targetSprCode)
	}

	def getRelationships(relationshipType: StudentRelationshipType, targetSprCode: String): Seq[StudentRelationship] = transactional(readOnly = true) {
		memberDao.getRelationshipsByTarget(relationshipType, targetSprCode)
	}

	def saveStudentRelationship(relationshipType: StudentRelationshipType, targetSprCode: String, agent: String): StudentRelationship = transactional() {
		this.findCurrentRelationships(relationshipType, targetSprCode).find(_.agent == agent) match {
			case Some(existingRelationship) => {
				// the same relationship is already there in the db - don't create new one
				existingRelationship.endDate = null
				memberDao.saveOrUpdate(existingRelationship)
				existingRelationship
			}
			case _ => {
				// create the new one
				val newRelationship = StudentRelationship(agent, relationshipType, targetSprCode)
				newRelationship.startDate = new DateTime
				memberDao.saveOrUpdate(newRelationship)
				newRelationship
			}
		}
	}
	
	def relationshipDepartmentFilterMatches(department: Department)(rel: StudentRelationship) = 
		rel.studentMember.exists(studentDepartmentFilterMatches(department))
	
	def relationshipNotPermanentlyWithdrawn(rel: StudentRelationship) =
		rel.studentMember.exists(studentNotPermanentlyWithdrawn)
	
	def studentDepartmentFilterMatches(department: Department)(member: StudentMember) = department.filterRule.matches(member)
	
	def studentNotPermanentlyWithdrawn(member: StudentMember) =
		member.mostSignificantCourseDetails
			 .flatMap(scd => Option(scd.sprStatus))
			 .exists(_.code != "P")

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
			.filter(studentNotPermanentlyWithdrawn)
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
}

trait RelationshipServiceComponent {
	val relationshipService: RelationshipService
}
trait AutowiringRelationshipServiceComponent extends RelationshipServiceComponent{
	val relationshipService = Wire[RelationshipService]
}
