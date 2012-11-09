package uk.ac.warwick.courses.data.model

import org.hibernate.annotations.AccessType
import javax.persistence._
import scala.annotation.target._

/** A MarkScheme defines how an assignment will be marked, including who
  * will be the markers and what rules should be used to decide how submssions
  * are distributed.
  *
  * A MarkScheme is created against a Department and can then be reused by
  * many Assignments within that Department.
  */
@Entity
@AccessType("field")
class MarkScheme extends GeneratedId {

	def this(dept: Department) = {
		this();
		this.department = dept
	}

	/** A descriptive name for the users's reference. */
	var name: String = null

	@ManyToOne
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

}