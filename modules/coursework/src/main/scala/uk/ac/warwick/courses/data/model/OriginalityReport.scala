package uk.ac.warwick.courses.data.model

import org.hibernate.annotations.Type
import javax.persistence.Table
import javax.persistence.Entity
import javax.persistence.OneToOne
import javax.persistence.Column
import javax.persistence.JoinColumn
import org.joda.time.DateTime

@Entity
class OriginalityReport() extends GeneratedId {

	@OneToOne(optional = false)
	@JoinColumn(name="ATTACHMENT_ID")
    var attachment: FileAttachment = _

	def completed = similarity map { _ > -1 } getOrElse false

	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	var createdDate: DateTime = DateTime.now

	@Type(`type` = "uk.ac.warwick.courses.data.model.OptionIntegerUserType")
	var similarity: Option[Int] = None

	@Type(`type` = "uk.ac.warwick.courses.data.model.OptionIntegerUserType")
	var overlap: Option[Int] = None

	@Type(`type` = "uk.ac.warwick.courses.data.model.OptionIntegerUserType")
	@Column(name = "STUDENT_OVERLAP")
	var studentOverlap: Option[Int] = None

	@Type(`type` = "uk.ac.warwick.courses.data.model.OptionIntegerUserType")
	@Column(name = "WEB_OVERLAP")
	var webOverlap: Option[Int] = None

	@Type(`type` = "uk.ac.warwick.courses.data.model.OptionIntegerUserType")
	@Column(name = "PUBLICATION_OVERLAP")
	var publicationOverlap: Option[Int] = None
}