package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model.{StudentMember, StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.spring.Wire
import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.{CommandInternal, Unaudited, ComposableCommand}
import uk.ac.warwick.tabula.services.AutowiringModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

class RelationshipFixtureCommand extends CommandInternal[StudentRelationship] {
	this: TransactionalComponent with SessionComponent=>

	val memberDao = Wire[MemberDao]
	var agent:String = _
	var studentUniId:String = _
	var relationshipType:String = "tutor"

	protected def applyInternal() =
		transactional() {
			val relType = memberDao.getStudentRelationshipTypeByUrlPart(relationshipType).get
			val studentSprCode = memberDao.getByUniversityId(studentUniId).get match {
				case x: StudentMember => x.mostSignificantCourseDetails.get.sprCode
				case _ => throw new RuntimeException(s"$studentUniId could not be resolved to a student member")
			}
			val existing = memberDao.getRelationshipsByAgent(relType, agent).find (_.targetSprCode == studentSprCode)

			val modifications = existing match {
				case Some(existingRel) =>{
					existingRel.endDate = null // make sure it hasn't expired
					existingRel
				}
				case None =>{
					val relationship = StudentRelationship(agent,relType,studentSprCode)
					relationship.startDate = DateTime.now()
					relationship
				}
			}
			session.saveOrUpdate(modifications)
			modifications
		}
}

object RelationshipFixtureCommand{
	def apply()={
		new RelationshipFixtureCommand
			with ComposableCommand[StudentRelationship]
			with AutowiringModuleAndDepartmentServiceComponent
			with Daoisms
			with AutowiringTransactionalComponent
			with Unaudited
			with PubliclyVisiblePermissions
	}
}
