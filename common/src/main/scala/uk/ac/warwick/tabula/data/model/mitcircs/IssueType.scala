package uk.ac.warwick.tabula.data.model.mitcircs


import enumeratum.EnumEntry
import enumeratum._
import uk.ac.warwick.tabula.data.model.EnumSeqUserType

import scala.collection.immutable
import uk.ac.warwick.tabula.system.EnumSeqTwoWayConverter

sealed abstract class IssueType(val description: String, val helpText: Option[String] = None) extends EnumEntry

object IssueType extends Enum[IssueType] {

  val values: immutable.IndexedSeq[IssueType] = findValues

  case object SeriousAccident extends IssueType(
    description = "Serious accident",
    helpText = Some("An accident which had a significant effect on your ability to complete an assessment. Normally the accident would have required you to receive medical treatment and would be supported by a doctor's (or other healthcare professional) note.")
  )
  case object SeriousPhysicalIllness extends IssueType(
    description = "Serious physical illness",
    helpText = Some("An illness that requires medication prescribed by a GP, or a referral to a specialist, supported by a doctor's (or other healthcare professional) note. Minor illnesses such as coughs and colds not requiring treatment would not normally count.")
  )
  case object MentalHealth extends IssueType(
    description = "Mental health issue",
    helpText = Some("A mental health issue that is more severe than typical, short-term assessment stress and anxiety, for which you're receiving (or have asked) for support from university or other mental health services, supported by a note from your support service.")
  )
  case object SeriousMedicalOther extends IssueType(
    description = "Serious accident or illness of someone close",
    helpText = Some("This would normally be a close family member, and would be supported by a doctor's note. Conditions which require you to undertake new and significant caring responsibilities are particularly relevant.")
  )
  case object Employment extends IssueType(
    description = "Significant changes in employment circumstances",
    helpText = Some("As a part-time student, if you’re also in employment and your employer makes changes beyond your control, eg to your working hours or your place of employment, this may count as mitigating circumstances.")
  )
  case object Deterioration extends IssueType(
    description = "Deterioration of a permanent condition",
    helpText = Some("A condition which you have already reported and is already covered by reasonable adjustments, but which has become significantly worse.")
  )
  case object Bereavement extends IssueType(
    description = "Bereavement",
    helpText = Some("The death of someone close to you (normally a close family member) around the time of an assessment, supported by a death certificate or funeral notice.")
  )
  case object AbruptChange extends IssueType(
    description = "Sudden change in personal circumstances",
    helpText = Some("Changes of this sort may include a divorce or separation, a fire, a court appearance, or an acute accommodation crisis.")
  )
  case object LateDiagnosis extends IssueType(
    description = "Late diagnosis of a specific learning difference",
    helpText = Some("If you have not previously been diagnosed with a disability (including a specific learning difference), but receive such a diagnosis close to an assessment, this may count as mitigating circumstances.")
  )
  case object VictimOfCrime extends IssueType(
    description = "Victim of crime",
    helpText = Some("If you are the victim of a crime (normally supported by a crime number provided by the police) which has caused you significant distress and/or practical difficulties, this may count as mitigating circumstances. Involvement in a criminal case as a witness may also count.")
  )
  case object Harassment extends IssueType(
    description = "Suffered bullying, harassment, victimisation or threatening behaviour",
    helpText = Some("If you have suffered behaviour which has caused you significant distress and which you have reported to an appropriate body, this may count as mitigating circumstances.")
  )
  case object IndustrialAction extends IssueType(
    description = "Industrial action",
    helpText = Some("If your studies are affected by industrial action (eg. your lectures or seminars get cancelled or rearranged) then this may count as mitigating circumstances. A statement of changes that have affected you, provided by your department, should normally be provided as supporting evidence.")
  )
  case object Other extends IssueType(
    description = "Other",
    helpText = Some("This list is not exhaustive and if you want to report a claim for mitigating circumstances which is not listed above, but does in in your opinion represent a mitigating circumstance, you should list it here.")
  )
}

class IssueTypeUserType extends EnumSeqUserType[IssueType]
class IssueTypeConverter extends EnumSeqTwoWayConverter[IssueType]