package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.Type
import org.joda.time.DateTime
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery

@Entity
@NamedQueries(Array(
	new NamedQuery(name = "status.code", query = "select sitsStatus from SitsStatus sitsStatus where code = :code")))
class SitsStatus {

	def this(code: String = null, shortName: String = null, fullName: String = null) {
		this()
		this.code = code
		this.shortName = shortName
		this.fullName = fullName
	}

	@Id var code: String = _
	var shortName: String = _
	var fullName: String = _

	@Type(`type` = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	var lastUpdatedDate = DateTime.now	
	
	override def toString = fullName.toLowerCase()

}
