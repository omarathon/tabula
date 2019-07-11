package uk.ac.warwick.tabula.data.model.mitcircs

import enumeratum.EnumEntry.CapitalWords
import enumeratum._
import uk.ac.warwick.tabula.data.model.EnumUserType
import uk.ac.warwick.tabula.system.EnumTwoWayConverter

import scala.collection.immutable

sealed abstract class MitigatingCircumstancesSubmissionState(val description: String) extends EnumEntry with CapitalWords
object MitigatingCircumstancesSubmissionState extends Enum[MitigatingCircumstancesSubmissionState] {
  case object Draft extends MitigatingCircumstancesSubmissionState("Draft")
  case object CreatedOnBehalfOfStudent extends MitigatingCircumstancesSubmissionState("Awaiting student sign-off")
  case object Submitted extends MitigatingCircumstancesSubmissionState("Submitted")
  case object ReadyForPanel extends MitigatingCircumstancesSubmissionState("Ready for panel")
  case object OutcomesRecorded extends MitigatingCircumstancesSubmissionState("Outcomes recorded")
  case object ApprovedByChair extends MitigatingCircumstancesSubmissionState("Approved by panel chair")
  case object Withdrawn extends MitigatingCircumstancesSubmissionState("Withdrawn by student")

  override val values: immutable.IndexedSeq[MitigatingCircumstancesSubmissionState] = findValues
}

class MitigatingCircumstancesSubmissionStateUserType extends EnumUserType(MitigatingCircumstancesSubmissionState)
class MitigatingCircumstancesSubmissionStateConverter extends EnumTwoWayConverter(MitigatingCircumstancesSubmissionState)
