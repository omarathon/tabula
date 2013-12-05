package uk.ac.warwick.tabula.data.model

import java.sql.Types
import org.hibernate.`type`.StandardBasicTypes
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Type
import org.joda.time.DateTime
import javax.persistence._
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{ToString, SprCode}
import org.springframework.dao.DataRetrievalFailureException
import uk.ac.warwick.tabula.JavaImports._

@Entity
@AccessType("field")
/*
 * The relationship is made up of an agent (e.g. tutor), a relationship type and
 * the SPR code of the student - so <some agent> is <some relationship e.g. personal tutor>
 * to <some spr code>
 */
class StudentRelationship extends GeneratedId {

	@transient var profileService = Wire.auto[ProfileService]

	// "agent" is the the actor in the relationship, e.g. tutor
	var agent: String = _

	@ManyToOne
	@JoinColumn(name = "relationship_type")
	var relationshipType: StudentRelationshipType = _

	@Column(name="target_sprcode")
	var targetSprCode: String = new String("")

	@Column(name = "uploaded_date")
	var uploadedDate: DateTime = new DateTime

	@Column(name = "start_date")
	var startDate: DateTime = _

	@Column(name = "end_date")
	var endDate: DateTime = _
	
	var percentage: JBigDecimal = null

	// assume that all-numeric value is a member (not proven though)
	def isAgentMember: Boolean = agent match {
		case null => false
		case a => a.forall(_.isDigit)
	}

	def agentMember: Option[Member] = isAgentMember match {
		case true => profileService.getMemberByUniversityId(agent)
		case false => None
	}

	/**
	 * If the agent matches a University ID, the Member is returned.
	 * Otherwise the agent string is returned.
	 *
	 * TODO wildcard return types are bad practice
	 */
	def agentParsed: Any = agentMember.getOrElse(agent)

	def agentName = agentMember.map( _.fullName.getOrElse("[Unknown]") ).getOrElse(agent)

	def agentLastName = agentMember.map( _.lastName ).getOrElse(agent) // can't reliably extract a last name from agent string

	def studentMember = profileService.getStudentBySprCode(targetSprCode) 

	def studentId = SprCode.getUniversityId(targetSprCode)

	override def toString = super.toString + ToString.forProps("agent" -> agent, "relationshipType" -> relationshipType, "student" -> targetSprCode)
}

object StudentRelationship {

	def apply(agent: String, relType: StudentRelationshipType, targetSprCode: String) = {

		val rel = new StudentRelationship
		rel.agent = agent
		rel.relationshipType = relType
		rel.targetSprCode = targetSprCode
		rel
	}

}