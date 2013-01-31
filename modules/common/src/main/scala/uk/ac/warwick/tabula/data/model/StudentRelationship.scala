package uk.ac.warwick.tabula.data.model

import java.sql.Types
import scala.reflect.BeanProperty
import org.hibernate.`type`.StandardBasicTypes
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Type
import org.joda.time.DateTime
import javax.persistence.Column
import javax.persistence.Entity
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.spring.Wire

@Entity
@AccessType("field")
/*
 * The relationship is made up of an agent (e.g. tutor), a relationship type and 
 * the SPR code of the student - so <some agent> is <some relationship e.g. personal tutor> 
 * to <some spr code>
 */
class StudentRelationship extends GeneratedId {
	
	@transient lazy val memberDao = Wire.auto[MemberDao]

	// "agent" is the the actor in the relationship, e.g. tutor
	@BeanProperty var agent: String = _
	
	@Column(name="relationship_type")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.RelationshipUserType")
	@BeanProperty var relationshipType: RelationshipType = PersonalTutor
	
	@Column(name="target_sprcode")
	@BeanProperty var targetSprCode: String = new String("")
	
	@Column(name = "uploaded_date")
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	var uploadedDate: DateTime = new DateTime

	@Column(name = "start_date")
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	var startDate: DateTime = _
	
	@Column(name = "end_date")
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	var endDate: DateTime = _
	
	// assume that all-numeric value is a member (not proven though)
	def isAgentMember: Boolean = agent.forall(_.isDigit)
	
	def getAgentMember: Option[Member] = {
			val profileService = Wire.auto[ProfileService]
			
			isAgentMember match {
				case true => profileService.getMemberByUniversityId(agent)
				case false => None
			}
	}
	
	def getAgentParsed = {
		getAgentMember match {
			case None => agent
			case Some(m) => m
		}
	}
	
	def getTarget = {
		memberDao.getBySprCode(targetSprCode) 
	}
}

object StudentRelationship {
	def apply(agent: String, relType: RelationshipType, targetSprCode: String) = {
		
		val mr = new StudentRelationship
		mr.agent = agent
		mr.relationshipType = relType
		mr.targetSprCode = targetSprCode
		mr
	}
}


sealed abstract class RelationshipType(val dbValue: String, @BeanProperty val description: String)
case object PersonalTutor extends RelationshipType("personalTutor", "Personal Tutor")

object RelationshipType {
	def fromCode(code: String) = code match {
	  	case PersonalTutor.dbValue => PersonalTutor
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