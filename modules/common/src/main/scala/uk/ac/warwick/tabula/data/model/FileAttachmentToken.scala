package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.joda.time.DateTime

@Entity
class FileAttachmentToken extends GeneratedId {

	val DefaultTokenValidityMinutes = 30

	@Column(name="fileattachment_id")
	var fileAttachmentId: String = _

	var expires: DateTime = _

	var used: Boolean = _

	def init(fileAttachment: FileAttachment): Unit = {
		this.fileAttachmentId = fileAttachment.id
		this.expires = new DateTime().plusMinutes(DefaultTokenValidityMinutes)
	}

}
