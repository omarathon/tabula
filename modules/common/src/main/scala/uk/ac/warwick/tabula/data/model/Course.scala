package uk.ac.warwick.tabula.data.model

import org.joda.time.DateTime
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery

@Entity
@NamedQueries(Array(
	new NamedQuery(name = "course.code", query = "select course from Course course where code = :code")))
class Course {

	def this(code: String = null, name: String = null) {
		this()
		this.code = code
		this.name = name
	}

	@Id var code: String = _
	var shortName: String = _
	var name: String = _
	var title: String = _

	var lastUpdatedDate: DateTime = DateTime.now

	override def toString: String = name

}
