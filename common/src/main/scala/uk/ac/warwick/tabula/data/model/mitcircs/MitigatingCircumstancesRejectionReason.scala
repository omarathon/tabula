package uk.ac.warwick.tabula.data.model.mitcircs

import enumeratum.{Enum, EnumEntry}
import uk.ac.warwick.tabula.data.model.EnumSeqUserType
import uk.ac.warwick.tabula.system.EnumTwoWayConverter

import scala.collection.immutable

sealed abstract class MitigatingCircumstancesRejectionReason(val description: String, val helpText: String) extends EnumEntry

object MitigatingCircumstancesRejectionReason extends Enum[MitigatingCircumstancesRejectionReason] {

  case object NoSupportingEvidence extends MitigatingCircumstancesRejectionReason(
    description = "The evidence submitted did not support the claim",
    helpText = "No evidence was presented to show that the nature of the circumstances was over and above the normal difficulties that would be experienced by an average person with average resilience."
  )

  case object NoIndependentEvidence extends MitigatingCircumstancesRejectionReason(
    description = "No independent documentary evidence was supplied to support the claim",
    helpText = "Letters from family and friends are not normally sufficient."
  )

  case object TimingNotAdverse extends MitigatingCircumstancesRejectionReason(
    description = "There was sufficient evidence to show that the timing of the circumstances would not have adversely affected the assessment",
    helpText = "Circumstances which are real but far away in time from the assessments being claimed for are unlikely to succeed."
  )

  case object ReasonableAdjustmentExists extends MitigatingCircumstancesRejectionReason(
    description = "Already mitigated via a reasonable adjustment",
    helpText = "The circumstance is a disability for which reasonable adjustments have already been made."
  )

  case object DuplicateSubmission extends MitigatingCircumstancesRejectionReason(
    description = "Sufficient mitigation has already been made for the same circumstances",
    helpText = "The same circumstances should not give rise to two separate mitigations."
  )

  case object Other extends MitigatingCircumstancesRejectionReason(
    description = "Other",
    helpText = "Describe any other reasons why this submission was rejected here."
  )

  override val values: immutable.IndexedSeq[MitigatingCircumstancesRejectionReason] = findValues
}

class MitigatingCircumstancesRejectionReasonUserType extends EnumSeqUserType(MitigatingCircumstancesRejectionReason)

class MitigatingCircumstancesRejectionReasonConverter extends EnumTwoWayConverter(MitigatingCircumstancesRejectionReason)