package uk.ac.warwick.tabula.data.model.mitcircs

import enumeratum.EnumEntry.CapitalWords
import enumeratum._
import uk.ac.warwick.tabula.data.model.EnumUserType
import uk.ac.warwick.tabula.system.EnumTwoWayConverter

import scala.collection.immutable

sealed abstract class MitigatingCircumstancesGrading(val code: String, val helpText: String) extends EnumEntry with CapitalWords {
  def description: String = code
}

object MitigatingCircumstancesGrading extends Enum[MitigatingCircumstancesGrading] {

  case object Rejected extends MitigatingCircumstancesGrading(
    code = "R",
    helpText = "The claim is rejected due to insufficient evidence, incomplete information, or does not meet the criteria for mitigating circumstances."
  )

  case object Mild extends MitigatingCircumstancesGrading(
    code = "A",
    helpText = "The mitigating circumstances were considered mild, and/or had little material effect on the student’s academic performance. For example, the circumstances fall within the normal level of everyday life that a person with normal emotional resilience would be expected to cope with. OR (b) There is weak evidence (or the evidence is post-hoc in nature) detailing the level of impact on the student making it impossible to assess the impact with reasonable certainty."
  )

  case object Moderate extends MitigatingCircumstancesGrading(
    code = "B",
    helpText = "Medical or other circumstances where substantial impairment of a student’s performance would be expected and is evidenced with some reasonable degree of certainty."
  )

  case object Severe extends MitigatingCircumstancesGrading(
    code = "C",
    helpText = "Severe circumstances which would be highly detrimental to a student’s academic performance and is evidenced with a high level of certainty."
  )

  override val values: immutable.IndexedSeq[MitigatingCircumstancesGrading] = findValues
}

class MitigatingCircumstancesGradingUserType extends EnumUserType(MitigatingCircumstancesGrading)
class MitigatingCircumstancesGradingConverter extends EnumTwoWayConverter(MitigatingCircumstancesGrading)
