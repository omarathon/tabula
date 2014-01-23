package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.joda.time.DateTime

import javax.validation.constraints.NotNull
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import java.text.BreakIterator

@Entity
@DiscriminatorColumn(name = "discriminator", discriminatorType = DiscriminatorType.STRING)
abstract class AttendanceNote extends GeneratedId with FormattedHtml {

	@ManyToOne
	@JoinColumn(name = "student_id")
	var student: StudentMember = _

	var updatedDate: DateTime = _

	@NotNull
	var updatedBy: String = _

	var note: String = _

	def escapedNote: String = formattedHtml(note)

	def truncatedNote: String = {
		Option(note).map{ note =>
			val breakIterator: BreakIterator = BreakIterator.getWordInstance
			breakIterator.setText(note)
			val length = Math.min(note.length, breakIterator.following(Math.min(50, note.length)))
			if (length < 0 || length == note.length) {
				note
			} else {
				note.substring(0, length) + "&hellip;"
			}
		}.getOrElse("")
	}

	@OneToOne
	@JoinColumn(name = "attachment_id")
	var attachment: FileAttachment = _

}