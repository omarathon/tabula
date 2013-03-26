package uk.ac.warwick.tabula.data.model

import scala.beans.BeanProperty
import org.hibernate.annotations.Type
import org.joda.time.DateTime
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery

@Entity
@NamedQueries(Array(
	new NamedQuery(name = "modeofattendance.code", query = "select modeOfAttendance from ModeOfAttendance modeOfAttendance where code = :code")))
class ModeOfAttendance {

	def this(code: String = null, shortName: String = null, fullName: String = null) {
		this()
		this.code = code
		this.shortName = shortName
		this.fullName = fullName
	}

	@Id @BeanProperty var code: String = _
	@BeanProperty var shortName: String = _
	@BeanProperty var fullName: String = _

	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var lastUpdatedDate = DateTime.now	
	
	override def toString = fullName.toLowerCase()
	
	def fullNameToDisplay = {
		if (code.equals("F"))
			"full time"
		else fullName.toLowerCase
	}

}
