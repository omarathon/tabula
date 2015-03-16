package uk.ac.warwick.tabula.data.model

import javax.persistence._
import javax.persistence.CascadeType._
import org.hibernate.annotations.DiscriminatorOptions

@Entity
@Table(name = "marker_usergroup")
@DiscriminatorColumn(name = "discriminator", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorOptions(force=true)
abstract class MarkerMap extends GeneratedId {

	@Column(name="marker_uni_id")
	var marker_id: String = _

	@OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "markermap_id")
	var students: UserGroup = _

}

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("first")
class FirstMarkersMap extends MarkerMap {

	// When this is on the superclass Hibernate throws an exception
	// as it only looks for the property on the concrete class
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignment_id")
	var assessment: Assessment = _

}

object FirstMarkersMap {
	def apply(assignment: Assignment, marker_id: String, students: UserGroup) = {
		val map = new FirstMarkersMap
		map.assessment = assignment
		map.marker_id = marker_id
		map.students = students
		map
	}
}

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("second")
class SecondMarkersMap extends MarkerMap {

	// See comment in FirstMarkersMap
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignment_id")
	var assessment: Assessment = _

}

object SecondMarkersMap {
	def apply(assignment: Assignment, marker_id: String, students: UserGroup) = {
		val map = new SecondMarkersMap
		map.assessment = assignment
		map.marker_id = marker_id
		map.students = students
		map
	}
}
