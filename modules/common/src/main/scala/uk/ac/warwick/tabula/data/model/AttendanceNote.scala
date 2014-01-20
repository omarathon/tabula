package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.joda.time.DateTime

import javax.validation.constraints.NotNull
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml

@Entity
@DiscriminatorColumn(name = "discriminator", discriminatorType = DiscriminatorType.STRING)
abstract class AttendanceNote extends GeneratedId with FormattedHtml {

	@ManyToOne
	@JoinColumn(name = "student_id")
	var student: StudentMember = _

	var updatedDate: DateTime = _

	@NotNull
	var updatedBy: String = _

	var note: String =_

	def escapedNote: String = formattedHtml(note)

	@OneToOne
	@JoinColumn(name = "attachment_id")
	var attachment: FileAttachment = _

}