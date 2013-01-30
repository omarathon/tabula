package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.AccessType
import javax.persistence._
import scala.collection.JavaConversions._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.SubmissionState._
import uk.ac.warwick.tabula.permissions.PermissionsTarget

/** A MarkScheme defines how an assignment will be marked, including who
  * will be the markers and what rules should be used to decide how submssions
  * are distributed.
  *
  * A MarkScheme is created against a Department and can then be reused by
  * many Assignments within that Department.
  */
@Entity
@AccessType("field")
class MarkScheme extends GeneratedId with PermissionsTarget {

	def this(dept: Department) = {
		this()
		this.department = dept
	}

	/** A descriptive name for the users's reference. */
	@Basic(optional = false)
	var name: String = null

	@ManyToOne(optional = false)
	@JoinColumn(name = "department_id")
	var department: Department = null
	
	def permissionsParents = Seq(Option(department)).flatten

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
	
	/** If true, the submitter chooses their first marker from a dropdown */
	var studentsChooseMarker = false

	def getSubmissions(assignment: Assignment, user: User): Seq[Submission] = {

		if(studentsChooseMarker) {
			// if studentsChooseMarker exists then a field will exist too so fetch it
			assignment.markerSelectField match {
				case Some(markerField) => {
					val releasedSubmission = assignment.submissions.filter(_.state == ReleasedForMarking)
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
		else Seq() //TODO - no defined behaviour for default mark schemes yet
	}

}