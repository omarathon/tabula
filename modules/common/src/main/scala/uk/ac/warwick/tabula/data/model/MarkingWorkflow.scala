package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.{Type, AccessType}
import javax.persistence._
import scala.collection.JavaConversions._
import uk.ac.warwick.userlookup.User
import scala.{Array, Some}
import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types
import org.springframework.core.convert.converter.Converter
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.permissions.PermissionsTarget

/** A MarkingWorkflow defines how an assignment will be marked, including who
  * will be the markers and what rules should be used to decide how submissions
  * are distributed.
  *
  * A MarkingWorkflow is created against a Department and can then be reused by
  * many Assignments within that Department.
  */
@Entity
@Table(name="MarkScheme")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="MarkingMethod", discriminatorType = DiscriminatorType.STRING, length=255)
@AccessType("field")
abstract class MarkingWorkflow extends GeneratedId with PermissionsTarget {

	@transient
	var userLookup = Wire[UserLookupService]("userLookup")

	/** A descriptive name for the users' reference. */
	@Basic(optional = false)
	var name: String = null

	@ManyToOne(optional = false)
	@JoinColumn(name = "department_id")
	var department: Department = null

	def permissionsParents = Option(department).toStream

	/** The group of first markers. */
	@OneToOne(cascade = Array(CascadeType.ALL))
	@JoinColumn(name = "firstmarkers_id")
	var firstMarkers = UserGroup.ofUsercodes

	/** The second group of markers. May be unused if the marking workflow
	  * only has one marking stage.
	  */
	@OneToOne(cascade = Array(CascadeType.ALL))
	@JoinColumn(name = "secondmarkers_id")
	var secondMarkers = UserGroup.ofUsercodes

	def markingMethod: MarkingMethod

	/** If true, the submitter chooses their first marker from a dropdown */
	def studentsChooseMarker = false

	// True if this marking workflow uses a second marker
	def hasSecondMarker: Boolean

	def studentHasMarker(assignment:Assignment, universityId: String): Boolean =
		getStudentsFirstMarker(assignment, universityId).isDefined || getStudentsSecondMarker(assignment, universityId).isDefined

	def getStudentsFirstMarker(assignment:Assignment, universityId: String): Option[String]

	def getStudentsSecondMarker(assignment:Assignment, universityId: String): Option[String]

	def getSubmissions(assignment: Assignment, user: User): Seq[Submission]

	override def toString = "MarkingWorkflow(" + id + ")"

}


/**
 * Available marking methods and code to persist them
 */

sealed abstract class MarkingMethod(val name: String){
	override def toString = name
}

object MarkingMethod {
	case object StudentsChooseMarker extends MarkingMethod("StudentsChooseMarker")
	case object SeenSecondMarking extends MarkingMethod("SeenSecondMarking")

	val values: Set[MarkingMethod] = Set(StudentsChooseMarker, SeenSecondMarking)

	def fromCode(code: String): MarkingMethod =
		if (code == null) null
		else values.find{_.name == code} match {
			case Some(method) => method
			case None => throw new IllegalArgumentException()
		}
}

class MarkingMethodUserType extends AbstractStringUserType[MarkingMethod]{
	override def convertToObject(string: String) = MarkingMethod.fromCode(string)
	override def convertToValue(state: MarkingMethod) = state.name
}

class StringToMarkingMethod extends Converter[String, MarkingMethod]{
	def convert(string:String):MarkingMethod = MarkingMethod.fromCode(string)
}
