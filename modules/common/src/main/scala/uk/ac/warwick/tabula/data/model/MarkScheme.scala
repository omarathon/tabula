package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.{Type, AccessType}
import javax.persistence._
import scala.collection.JavaConversions._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.JavaImports._
import scala.{Array, Some}
import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types
import reflect.BeanProperty
import org.springframework.core.convert.converter.Converter
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserLookupService

/** A MarkScheme defines how an assignment will be marked, including who
  * will be the markers and what rules should be used to decide how submissions
  * are distributed.
  *
  * A MarkScheme is created against a Department and can then be reused by
  * many Assignments within that Department.
  */
@Entity
@AccessType("field")
class MarkScheme extends GeneratedId {

	@transient
	var userLookup = Wire[UserLookupService]("userLookup")

	def this(dept: Department) = {
		this()
		this.department = dept
	}

	/** A descriptive name for the users' reference. */
	@Basic(optional = false)
	var name: String = null

	@ManyToOne(optional = false)
	@JoinColumn(name = "department_id")
	var department: Department = null

	/** The group of first markers. */
	@OneToOne(cascade = Array(CascadeType.ALL))
	@JoinColumn(name = "firstmarkers_id")
	var firstMarkers = new UserGroup()

	/** The second group of markers. May be unused if the mark scheme
	  * only has one marking stage.
	  */
	@OneToOne(cascade = Array(CascadeType.ALL))
	@JoinColumn(name = "secondmarkers_id")
	var secondMarkers = new UserGroup()

	@Type(`type` = "uk.ac.warwick.tabula.data.model.MarkingMethodUserType")
	@BeanProperty var markingMethod: MarkingMethod = _

	/** If true, the submitter chooses their first marker from a dropdown */
	def studentsChooseMarker = markingMethod == StudentsChooseMarker


	def getSubmissions(assignment: Assignment, user: User): Seq[Submission] = {
		if(studentsChooseMarker) {
			// if studentsChooseMarker exists then a field will exist too so fetch it
			assignment.markerSelectField match {
				case Some(markerField) => {
					val releasedSubmission = assignment.submissions.filter(_.isReleasedForMarking)
					releasedSubmission.filter(submission => {
						submission.getValue(markerField) match {
							case Some(subValue) => user.getUserId == subValue.value
							case None => false
						}
					})
				}
				case None => Seq()
			}
		}
		else {
			val isFirstMarker = assignment.isFirstMarker(user)
			val isSecondMarker = assignment.isSecondMarker(user)
			val submissionIds:JList[String] = assignment.markerMap.get(user.getUserId).includeUsers
			if(isFirstMarker)
				assignment.submissions.filter(s => submissionIds.exists(_ == s.userId) && s.isReleasedForMarking)
			else if(isSecondMarker)
				assignment.submissions.filter(s => submissionIds.exists(_ == s.userId) && s.isReleasedToSecondMarker)
			else
				Seq()
		}
	}

}


/**
 * Available marking methods and code to persist them
 */

sealed abstract class MarkingMethod(val name: String){
	override def toString = name
}

case object StudentsChooseMarker extends MarkingMethod("StudentsChooseMarker")
case object SeenSecondMarking extends MarkingMethod("SeenSecondMarking")

object MarkingMethod {
	val values: Set[MarkingMethod] = Set(StudentsChooseMarker, SeenSecondMarking)

	def fromCode(code: String): MarkingMethod =
		if (code == null) null
		else values.find{_.name == code} match {
			case Some(method) => method
			case None => throw new IllegalArgumentException()
		}
}

class MarkingMethodUserType extends AbstractBasicUserType[MarkingMethod, String]{

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String) = MarkingMethod.fromCode(string)
	override def convertToValue(state: MarkingMethod) = state.name
}

class StringToMarkingMethod extends Converter[String, MarkingMethod]{
	def convert(string:String):MarkingMethod = MarkingMethod.fromCode(string)
}
