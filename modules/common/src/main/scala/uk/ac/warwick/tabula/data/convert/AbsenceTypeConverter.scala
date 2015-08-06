package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.data.model.AbsenceType

class AbsenceTypeConverter extends TwoWayConverter[String, AbsenceType] {

	override def convertRight(value: String) =
		if (value.hasText) AbsenceType.fromCode(value)
		else null

	override def convertLeft(absenceType: AbsenceType) = Option(absenceType).map { _.dbValue }.orNull

}