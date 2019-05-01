package uk.ac.warwick.tabula.data.model.mitcircs

import enumeratum.EnumEntry.CapitalWords
import enumeratum._
import uk.ac.warwick.tabula.data.model.EnumUserType
import uk.ac.warwick.tabula.system.EnumTwoWayConverter

import scala.collection.immutable

sealed trait MitigatingCircumstancesSubmissionState extends EnumEntry with CapitalWords
object MitigatingCircumstancesSubmissionState extends Enum[MitigatingCircumstancesSubmissionState] {
  case object Draft extends MitigatingCircumstancesSubmissionState
  case object CreatedOnBehalfOfStudent extends MitigatingCircumstancesSubmissionState
  case object Submitted extends MitigatingCircumstancesSubmissionState

  override val values: immutable.IndexedSeq[MitigatingCircumstancesSubmissionState] = findValues
}

class MitigatingCircumstancesSubmissionStateUserType extends EnumUserType(MitigatingCircumstancesSubmissionState)
class MitigatingCircumstancesSubmissionStateConverter extends EnumTwoWayConverter(MitigatingCircumstancesSubmissionState)
