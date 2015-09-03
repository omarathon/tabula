package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.Type
import javax.persistence._
import org.joda.time.DateTime
import javax.persistence.Column

@Entity
class OriginalityReport extends GeneratedId with ToEntityReference {
	type Entity = OriginalityReport

	// Don't cascade as this is the wrong side of the association
	@OneToOne(optional = false, cascade=Array(), fetch = FetchType.LAZY)
	@JoinColumn(name="ATTACHMENT_ID")
  var attachment: FileAttachment = _

	def completed = similarity map { _ > -1 } getOrElse false

	var createdDate: DateTime = DateTime.now

	@Column(name = "TURNITIN_ID")
	var turnitinId: String = _

	@Column(name = "REPORT_RECEIVED")
	var reportReceived: Boolean = _

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

	override def toEntityReference = new OriginalityReportEntityReference().put(this)
}