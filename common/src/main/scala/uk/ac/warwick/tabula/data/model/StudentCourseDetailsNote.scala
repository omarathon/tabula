package uk.ac.warwick.tabula.data.model

import javax.persistence.{Entity, Id}
import org.hibernate.annotations.Proxy

@Entity
@Proxy(`lazy` = false)
class StudentCourseDetailsNote {

  def this(code: String, scjCode: String, note: String) = {
    this()
    this.code = code
    this.scjCode = scjCode
    this.note = note
  }

  @Id
  var code: String = _

  var scjCode: String = _

  var note: String = _
}

