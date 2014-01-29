package uk.ac.warwick.tabula.data.model

import org.joda.time.DateTime
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class Disability {

	def this(code: String = null, definition: String = null) {
		this()
		this.code = code
		this.definition = definition
	}

	@Id var code: String = _
	var shortName: String = _
	var definition: String = _

	var lastUpdatedDate = DateTime.now

	override def toString = definition
}
