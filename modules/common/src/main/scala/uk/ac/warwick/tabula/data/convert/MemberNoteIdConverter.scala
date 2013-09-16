package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.data.model.MemberNote
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.MemberNoteService


class MemberNoteIdConverter extends TwoWayConverter[String, MemberNote] {

	var service = Wire.auto[MemberNoteService]

	override def convertRight(id: String) = (Option(id) flatMap { service.getNoteById(_) }).orNull

	override def convertLeft(memberNote: MemberNote) = (Option(memberNote) map {_.id}).orNull

}