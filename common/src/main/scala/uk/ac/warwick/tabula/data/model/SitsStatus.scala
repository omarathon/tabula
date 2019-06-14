package uk.ac.warwick.tabula.data.model

import javax.persistence.{Entity, Id, NamedQueries, NamedQuery}
import org.hibernate.annotations.Proxy
import org.joda.time.DateTime

@Entity
@Proxy
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

  var lastUpdatedDate: DateTime = DateTime.now

  override def toString: String = fullName.toLowerCase()

}
