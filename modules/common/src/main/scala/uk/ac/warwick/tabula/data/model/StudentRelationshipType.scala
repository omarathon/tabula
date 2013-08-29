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
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.StringUtils._
import java.net.URLEncoder
import javax.validation.constraints.NotNull
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.spring.Wire

@Entity @AccessType("field")
class StudentRelationshipType extends PermissionsTarget with PermissionsSelector[StudentRelationshipType] {
	
	@Id
	var id: String = _
	
	/**
	 * The URL identifier for this type. Valid URL path chars only please!!
	 */
	@NotNull
	var urlPart: String = _
	
	/**
	 * What you'd call a single actor in this relationship's context, e.g. "personal tutor".
	 * 
	 * This should be lowercase, as it will be used in a sentence. If it's used in other places,
	 * the template is expected to capitalise accordingly.
	 */
	@NotNull
	var agentRole: String = _
	
	/**
	 * What you'd call a single student in this relationship's context, e.g. "personal tutee"
	 * 
	 * This should be lowercase, as it will be used in a sentence. If it's used in other places,
	 * the template is expected to capitalise accordingly.
	 */
	@NotNull
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
	
	var defaultDisplay: JBoolean = true
	
	@Column(name="expected_ug")
	var expectedUG: JBoolean = false
	
	@Column(name="expected_pgt")
	var expectedPGT: JBoolean = false
	
	@Column(name="expected_pgr")
	var expectedPGR: JBoolean = false
	
	@Column(name="sort_order")
	var sortOrder: Int = 2
	
	/**
	 * Do we expect this member to have a relationship of this type? Controls 
	 * whether it is hidden when empty, or displayed with a prompt to add.
	 */
	def displayIfEmpty(studentCourseDetails: StudentCourseDetails) = studentCourseDetails.courseType match {
		case CourseType.UG => expectedUG
		case CourseType.PGT => expectedPGT
		case CourseType.PGR => expectedPGR
		case _ => false
	}
	
	/**
	 * If the source is anything other than local, then this relationship type is read-only
	 */
	def readOnly(department: Department) = 
		(department.getStudentRelationshipSource(this) != StudentRelationshipSource.Local)
		
	@transient
	var relationshipService = Wire[RelationshipService]
		
	def empty = relationshipService.countStudentsByRelationship(this) == 0
	
	def permissionsParents = Stream.empty

	override def toString = "StudentRelationshipType(%s)".format(id)

}

object StudentRelationshipType {
	def apply(id: String, urlPart: String, agentRole: String, studentRole: String) = {
		val relType = new StudentRelationshipType
		relType.id = id
		relType.urlPart = urlPart
		relType.agentRole = agentRole
		relType.studentRole = studentRole
		relType
	}
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