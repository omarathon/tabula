package uk.ac.warwick.tabula.data.model

import java.sql.Types
import org.hibernate.`type`.StandardBasicTypes
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Type
import org.joda.time.DateTime
import javax.persistence.Column
import javax.persistence.Entity
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{ToString, SprCode}
import org.springframework.dao.DataRetrievalFailureException
import uk.ac.warwick.tabula.roles.Supervisor

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

	@Column(name="relationship_type")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.RelationshipUserType")
	var relationshipType: RelationshipType = RelationshipType.PersonalTutor

	@Column(name="target_sprcode")
	var targetSprCode: String = new String("")

	@Column(name = "uploaded_date")
	@Type(`type` = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	var uploadedDate: DateTime = new DateTime

	@Column(name = "start_date")
	@Type(`type` = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	var startDate: DateTime = _

	@Column(name = "end_date")
	@Type(`type` = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	var endDate: DateTime = _

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
	@transient var profileService = Wire.auto[ProfileService]

	def apply(agent: String, relType: RelationshipType, targetSprCode: String) = {

		val rel = new StudentRelationship
		rel.agent = agent
		rel.relationshipType = relType
		rel.targetSprCode = targetSprCode
		rel
	}

	def getLastNameFromAgent(agent: String) = {
		if (agent.forall(_.isDigit)) {
			profileService.getMemberByUniversityId(agent) match {
				case None => agent
				case Some(member) => member.lastName
			}
		} else {
			agent
		}
	}
}



sealed abstract class RelationshipType(val dbValue: String, val description: String)

object RelationshipType {
	case object PersonalTutor extends RelationshipType("personalTutor", "Personal Tutor")
	case object Supervisor extends RelationshipType("supervisor", "Supervisor")

	def fromCode(code: String) = code match {
	  	case PersonalTutor.dbValue => PersonalTutor
			case "tutor"=>PersonalTutor // match the term used in URLs
	  	case Supervisor.dbValue => Supervisor
	  	case null => null
	  	case _ => throw new IllegalArgumentException()
	}
}


class RelationshipUserType extends AbstractBasicUserType[RelationshipType, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String) = RelationshipType.fromCode(string)

	override def convertToValue(relType: RelationshipType) = relType.dbValue

}