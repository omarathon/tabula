package uk.ac.warwick.tabula.services

import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.MemberUserType
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.RelationshipType._
import uk.ac.warwick.tabula.data.model.RelationshipType
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.PrsCode
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.StudentCourseDetails

/**
 * Service providing access to members and profiles.
 */
trait RelationshipService {
	def saveOrUpdate(relationship: StudentRelationship)
	def findCurrentRelationships(relationshipType: RelationshipType, targetSprCode: String): Seq[StudentRelationship]
	def getRelationships(relationshipType: RelationshipType, targetUniversityId: String): Seq[StudentRelationship]
	def saveStudentRelationship(relationshipType: RelationshipType, targetSprCode: String, agent: String): StudentRelationship
	def listStudentRelationshipsByDepartment(relationshipType: RelationshipType, department: Department): Seq[StudentRelationship]
	def listStudentRelationshipsWithMember(relationshipType: RelationshipType, agent: Member): Seq[StudentRelationship]
	def listStudentRelationshipsWithUniversityId(relationshipType: RelationshipType, agentId: String): Seq[StudentRelationship]
	def listStudentsWithoutRelationship(relationshipType: RelationshipType, department: Department): Seq[Member]
	def countStudentsByRelationshipAndDepartment(relationshipType: RelationshipType, department: Department): (Int, Int)
}

@Service(value = "relationshipService")
class RelationshipServiceImpl extends RelationshipService with Logging {

	var memberDao = Wire.auto[MemberDao]
	var profileService = Wire.auto[ProfileService]
	var profileIndexService = Wire.auto[ProfileIndexService]

	def saveOrUpdate(relationship: StudentRelationship) = memberDao.saveOrUpdate(relationship)

	def findCurrentRelationships(relationshipType: RelationshipType, student: StudentMember): Seq[StudentRelationship] = transactional() {
		student.studentCourseDetails.asScala.flatMap {
			courseDetail => memberDao.getCurrentRelationships(relationshipType, courseDetail.sprCode)
		}
	}

	def findCurrentRelationships(relationshipType: RelationshipType, targetSprCode: String): Seq[StudentRelationship] = transactional() {
		memberDao.getCurrentRelationships(relationshipType, targetSprCode)
	}

	def getRelationships(relationshipType: RelationshipType, targetSprCode: String): Seq[StudentRelationship] = transactional(readOnly = true) {
		memberDao.getRelationshipsByTarget(relationshipType, targetSprCode)
	}

	def getPersonalTutors(studentCourseDetails: StudentCourseDetails): Seq[Member] = {
		val currentRelationship = findCurrentRelationships(PersonalTutor, studentCourseDetails.sprCode)
		currentRelationship.flatMap { rel => profileService.getMemberByUniversityId(rel.agent) }
	}

	def saveStudentRelationship(relationshipType: RelationshipType, targetSprCode: String, agent: String): StudentRelationship = transactional() {
		this.findCurrentRelationships(relationshipType, targetSprCode).find(_.agent == agent) match {
			case Some(existingRelationship) => {
				// the same relationship is already there in the db - don't save
				existingRelationship
			}
			case _ => {
				// TODO handle existing relationships?
				// and then create the new one
				val newRelationship = StudentRelationship(agent, relationshipType, targetSprCode)
				newRelationship.startDate = new DateTime
				memberDao.saveOrUpdate(newRelationship)
				newRelationship
			}
		}
	}

	def listStudentRelationshipsByDepartment(relationshipType: RelationshipType, department: Department) = transactional(readOnly=true) {
		memberDao.getRelationshipsByDepartment(relationshipType, department)
	}

	def listStudentRelationshipsWithMember(relationshipType: RelationshipType, agent: Member) = transactional(readOnly = true) {
		memberDao.getRelationshipsByAgent(relationshipType, agent.universityId)
	}

	def listStudentRelationshipsWithUniversityId(relationshipType: RelationshipType, agentId: String) = transactional(readOnly = true) {
		memberDao.getRelationshipsByAgent(relationshipType, agentId)
	}

  def listStudentsWithoutRelationship(relationshipType: RelationshipType, department: Department) = transactional(readOnly = true) {
		memberDao.getStudentsWithoutRelationshipByDepartment(relationshipType, department)
	}

  def countStudentsByRelationshipAndDepartment(relationshipType: RelationshipType, department: Department): (Int, Int) = transactional(readOnly = true) {
		(memberDao.countStudentsByDepartment(department).intValue, memberDao.countStudentsByRelationshipAndDepartment(relationshipType, department).intValue)
	}
}

trait RelationshipServiceComponent {
	var relationshipService:RelationshipService
}
trait AutowiringRelationshipServiceComponent extends RelationshipServiceComponent{
	var relationshipService = Wire[RelationshipService]
}
