package uk.ac.warwick.tabula.data.model.mitcircs

import enumeratum._
import uk.ac.warwick.tabula.data.model.EnumSeqUserType
import uk.ac.warwick.tabula.system.EnumTwoWayConverter

import scala.collection.immutable

sealed abstract class MitCircsExamBoardRecommendation(val description: String, val helpText: String) extends EnumEntry

object MitCircsExamBoardRecommendation extends Enum[MitCircsExamBoardRecommendation] {

  case object Mild extends MitCircsExamBoardRecommendation(
    description = "Mild",
    helpText = "The mitigating circumstances were considered mild, and/or had little material effect on the student’s academic performance. For example, the circumstances fall within the normal level of everyday life that a person with normal emotional resilience would be expected to cope with."
  )

  case object WaivePenalties extends MitCircsExamBoardRecommendation(
    description = "Waive penalties",
    helpText = "Waive or reduce penalties for late submission of assessed work."
  )

  case object WaiveAssessment extends MitCircsExamBoardRecommendation(
    description = "Waive assessment",
    helpText = "A student who has failed to submit a piece of work for assessment with a credit weighting of 3 credits or less may have that piece of assessment waived if the Board of Examiners concludes it is not in the student’s interest (or it is not possible) to reschedule it. The unreliable component will be disregarded and the module mark will be recalculated."
  )

  case object FurtherResit extends MitCircsExamBoardRecommendation(
    description = "Further resit",
    helpText = "Allow further re-sit (examination)/re-submit (assessed work) opportunity. This would be as a final attempt so the marks will be capped at the pass mark and there will be no further opportunity to remedy failure."
  )

  case object FurtherSit extends MitCircsExamBoardRecommendation(
    description = "Further first sit",
    helpText = "Allow a further sit (examination)/submit (assessed work) opportunity. This would be as a first attempt so marks will not be capped and there will be a further opportunity to remedy failure. Any marks achieved in the subsequent attempt will count as the original mark."
  )

  case object ProceedWithLowCredit extends MitCircsExamBoardRecommendation(
    description = "Proceed with low credit",
    helpText = "Proceed with low credit to the next year of study. This decision must be made within University and Programme Regulations. Students must be notified of the implications this has on any future failure and for the achievement of their degree."
  )

  case object AwardHigherDegree extends MitCircsExamBoardRecommendation(
    description = "Award higher degree",
    helpText = "Subject to any restrictions imposed by accreditation or professional certification, recommend to award a Degree (or other qualification), or award of a higher class of degree than would be merited by the marks returned."
  )

  case object RepeatYearAsFinalAttempt extends MitCircsExamBoardRecommendation(
    description = "Repeat year as final attempt",
    helpText = "Recommend to the to the Academic Registrar that the student should be granted a repeat of the year in full as a final attempt so that the marks are capped at the pass mark and there will be no further attempt to remedy failure. Note this will incur another set of fees."
  )

  case object RepeatYearAsFirstAttempt extends MitCircsExamBoardRecommendation(
    description = "Repeat year as first attempt",
    helpText = "Recommend to the Academic Registrar that the student should be granted a repeat of the year in full as a first attempt so that marks will not be capped (except for the MBChB programme) and there will be a further attempt to remedy failure. All previous marks achieved will be discounted. Note this will incur another set of fees."
  )

  case object CarryMitigationForward extends MitCircsExamBoardRecommendation(
    description = "Carry mitigation forward",
    helpText = "No action is required in terms of progress decisions, but the circumstances will be carried forward and be considered when determining the degree classification at the relevant level and at a future meeting of the Board of Examiners."
  )

  case object Other extends MitCircsExamBoardRecommendation(
    description = "Other",
    helpText = "If there are any further recommendations that should be considered by the exam board then describe them here."
  )

  override val values: immutable.IndexedSeq[MitCircsExamBoardRecommendation] = findValues
}

class MitCircsExamBoardRecommendationUserType extends EnumSeqUserType(MitCircsExamBoardRecommendation)

class MitCircsExamBoardRecommendationConverter extends EnumTwoWayConverter(MitCircsExamBoardRecommendation)
