package uk.ac.warwick.tabula.data.model.mitcircs

import enumeratum.{Enum, EnumEntry}
import uk.ac.warwick.tabula.data.model.EnumUserType
import uk.ac.warwick.tabula.system.EnumTwoWayConverter

import scala.collection.immutable

sealed abstract class MitigatingCircumstancesAcuteOutcome(val description: String) extends EnumEntry

object MitigatingCircumstancesAcuteOutcome extends Enum[MitigatingCircumstancesAcuteOutcome] {

  case object WaiveLatePenalties extends MitigatingCircumstancesAcuteOutcome(
    description = "Waive late submission penalties"
  )

  case object Extension extends MitigatingCircumstancesAcuteOutcome(
    description = "Grant extensions"
  )

  case object HandledElsewhere extends MitigatingCircumstancesAcuteOutcome(
    description = "Handled elsewhere"
  )

  override val values: immutable.IndexedSeq[MitigatingCircumstancesAcuteOutcome] = findValues
}

class MitigatingCircumstancesAcuteOutcomeUserType extends EnumUserType(MitigatingCircumstancesAcuteOutcome)

class MitigatingCircumstancesAcuteOutcomeConverter extends EnumTwoWayConverter(MitigatingCircumstancesAcuteOutcome)