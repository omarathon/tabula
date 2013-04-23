package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.Type
import javax.persistence._
import javax.persistence.CascadeType._
import org.joda.time.DateTime

@Entity
class OriginalityReport() extends GeneratedId {

	// Don't cascade as this is the wrong side of the association
	@OneToOne(optional = false, cascade=Array())
	@JoinColumn(name="ATTACHMENT_ID")
    var attachment: FileAttachment = _

	def completed = similarity map { _ > -1 } getOrElse false

	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	var createdDate: DateTime = DateTime.now

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
	var similarity: Option[Int] = None

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
	var overlap: Option[Int] = None

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
	@Column(name = "STUDENT_OVERLAP")
	var studentOverlap: Option[Int] = None

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
	@Column(name = "WEB_OVERLAP")
	var webOverlap: Option[Int] = None

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
	@Column(name = "PUBLICATION_OVERLAP")
	var publicationOverlap: Option[Int] = None
}