package uk.ac.warwick.tabula.data.model

import scala.reflect.BeanProperty
import org.hibernate.annotations.Type
import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types
import javax.persistence._
import org.hibernate.annotations.AccessType

@Entity
@AccessType("field")
class MemberRelationship extends GeneratedId {
	
	@BeanProperty var agent: String = new String("")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.RelationshipUserType") @BeanProperty var relationshipType: RelationshipType	= PersonalTutor
	@BeanProperty var subjectUniversityId: String = new String("")
	
	def init(agent: String, relType: RelationshipType, subjectUniversityId: String) {
		this.agent = agent
		this.relationshipType = relType
		this.subjectUniversityId = subjectUniversityId
	}
}

/*class MemberRelationship(
			@BeanProperty var agent: String, 
			@Type(`type` = "uk.ac.warwick.tabula.data.model.RelationshipUserType") @BeanProperty var relationshipType: RelationshipType, 
			@BeanProperty var subjectUniversityId: String) 
		extends GeneratedId {
	
	private def this() {
		// stupid hibernate
		this(null, null, null)
	}
}*/


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