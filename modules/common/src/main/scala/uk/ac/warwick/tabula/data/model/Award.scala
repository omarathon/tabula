package uk.ac.warwick.tabula.data.model

import org.joda.time.DateTime
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery

@Entity
@NamedQueries(Array(
	new NamedQuery(name = "award.code", query = "select award from Award award where code = :code")))
class Award {

	def this(code: String = null, name: String = null) {
		this()
		this.code = code
		this.name = name
	}

	@Id var code: String = _
	var shortName: String = _
	var name: String = _

	var lastUpdatedDate = DateTime.now

	override def toString = name

}
