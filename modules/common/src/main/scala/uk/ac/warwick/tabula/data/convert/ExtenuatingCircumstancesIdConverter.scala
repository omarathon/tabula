package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.ExtenuatingCircumstances
import uk.ac.warwick.tabula.services.MemberNoteService
import uk.ac.warwick.tabula.system.TwoWayConverter


class ExtenuatingCircumstancesIdConverter extends TwoWayConverter[String, ExtenuatingCircumstances] {

	var service = Wire.auto[MemberNoteService]

	override def convertRight(id: String) = service.getExtenuatingCircumstancesById(id).orNull

	override def convertLeft(circumstances: ExtenuatingCircumstances) = (Option(circumstances) map {_.id}).orNull

}