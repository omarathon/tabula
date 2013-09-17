package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.data.model.MemberNote
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.MemberNoteService


class MemberNoteIdConverter extends TwoWayConverter[String, MemberNote] {

	var service = Wire.auto[MemberNoteService]

	override def convertRight(id: String) = service.getNoteById(id).orNull

	override def convertLeft(note: MemberNote) = (Option(note) map {_.id}).orNull

}