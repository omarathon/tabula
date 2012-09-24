package uk.ac.warwick.courses.data.model.forms

import org.hibernate.annotations.{ Type, AccessType }
import uk.ac.warwick.courses.data.model.{ Assignment, GeneratedId }
import uk.ac.warwick.courses.actions.Deleteable
import scala.Array
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.CascadeType._
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.validation.constraints.NotNull
import reflect.BeanProperty
import org.joda.time.DateTime

@Entity @AccessType("field")
class Extension extends GeneratedId with Deleteable {

	def this(universityId: String = null) {
		this()
		this.universityId = universityId
	}

	@ManyToOne(optional = false, cascade = Array(PERSIST, MERGE), fetch = FetchType.LAZY)
	@JoinColumn(name = "assignment_id")
	@BeanProperty var assignment: Assignment = _

	@NotNull
	@BeanProperty var userId: String = _

	@NotNull
	@BeanProperty var universityId: String = _

	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var expiryDate: DateTime = _

	@BeanProperty var reason: String = _

	@BeanProperty var approved: Boolean = false

	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var approvedOn: DateTime = _

	@BeanProperty var approvalComments: String = _

}
