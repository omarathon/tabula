package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.AccessType
import javax.persistence._
import javax.persistence.CascadeType._
import javax.persistence.FetchType._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import org.hibernate.`type`.StandardBasicTypes
import org.hibernate.annotations.Type
import java.sql.Types
import uk.ac.warwick.tabula.permissions.PermissionsSelector

@Entity @AccessType("field")
class StudentRelationshipType extends PermissionsTarget with PermissionsSelector[StudentRelationshipType] {
	
	/**
	 * The ID is also often used in URL construction, so you may have /tutor/all for id=tutor
	 */
	@Id
	var id: String = _
	
	/**
	 * What you'd call a single actor in this relationship's context, e.g. "personal tutor".
	 * 
	 * This should be lowercase, as it will be used in a sentence. If it's used in other places,
	 * the template is expected to capitalise accordingly.
	 */
	var actorRole: String = _
	
	/**
	 * What you'd call a single student in this relationship's context, e.g. "personal tutee"
	 * 
	 * This should be lowercase, as it will be used in a sentence. If it's used in other places,
	 * the template is expected to capitalise accordingly.
	 */
	var studentRole: String = _
	
	/**
	 * A description of this relationship type
	 */
	var description: String = _
	
	/**
	 * The default source for this relationship's information
	 */
	@Type(`type` = "uk.ac.warwick.tabula.data.model.StudentRelationshipSourceUserType")
	var defaultSource: StudentRelationshipSource = StudentRelationshipSource.Local
	
	/**
	 * If the source is anything other than local, then this relationship type is read-only
	 */
	def readOnly(department: Department) = 
		(department.getStudentRelationshipSource(this) != StudentRelationshipSource.Local)
	
	def permissionsParents = Stream.empty

	override def toString = "StudentRelationshipType(%s)".format(id)

}

sealed abstract class StudentRelationshipSource(val dbValue: String)

object StudentRelationshipSource {
	case object Local extends StudentRelationshipSource("local")
	case object SITS extends StudentRelationshipSource("sits")

	def fromCode(code: String) = code match {
	  	case Local.dbValue => Local
	  	case SITS.dbValue => SITS
	  	case null => null
	  	case _ => throw new IllegalArgumentException()
	}
}

class StudentRelationshipSourceUserType extends AbstractBasicUserType[StudentRelationshipSource, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String) = StudentRelationshipSource.fromCode(string)

	override def convertToValue(relType: StudentRelationshipSource) = relType.dbValue

}