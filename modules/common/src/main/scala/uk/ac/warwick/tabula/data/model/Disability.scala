package uk.ac.warwick.tabula.data.model

import org.joda.time.DateTime
import javax.persistence.{Column, Entity, Id}

@Entity
class Disability {

	def this(code: String = null, definition: String = null) {
		this()
		this.code = code
		this.sitsDefinition = definition
	}

	@Id var code: String = _
	var shortName: String = _
	var sitsDefinition: String = _
	var tabulaDefinition: String = _
	var lastUpdatedDate = DateTime.now

	/**
	 * use our override out of preference, where available.
	 * It's manually maintained in the database.
	 */
	def definition = Option(tabulaDefinition).getOrElse(sitsDefinition)

	override def toString = definition
}
