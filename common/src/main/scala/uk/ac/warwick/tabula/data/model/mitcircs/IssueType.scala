package uk.ac.warwick.tabula.data.model.mitcircs

import enumeratum.EnumEntry
import enumeratum._
import uk.ac.warwick.tabula.data.model.{EnumSeqUserType, StudentMember}

import scala.collection.immutable
import uk.ac.warwick.tabula.system.EnumTwoWayConverter

sealed abstract class IssueType(val description: String, val helpText: String, val evidenceGuidance: String) extends EnumEntry

object IssueType extends Enum[IssueType] {

  val values: immutable.IndexedSeq[IssueType] = findValues

  case object SeriousAccident extends IssueType(
    description = "Serious accident",
    helpText = "An accident which had a significant effect on your ability to complete an assessment. Normally the accident would have required you to receive medical treatment and would be supported by a doctor's (or other healthcare professional) note.",
    evidenceGuidance = "A letter from a qualified health professional (e.g. medical doctor, nurse) on official, headed paper or with an official stamp. It must confirm the accident with dates, and must be recorded at the time of the accident, and must indicate the impact on the student."
  )
  case object SeriousPhysicalIllness extends IssueType(
    description = "Serious physical illness",
    helpText = "An illness that requires medication prescribed by a GP, or a referral to a specialist, supported by a doctor's (or other healthcare professional) note. Minor illnesses such as coughs and colds not requiring treatment would not normally count.",
    evidenceGuidance = "A letter from a qualified health professional (e.g. medical doctor, nurse) on official, headed paper or with an official stamp. It must confirm the illness with dates, and must be recorded at the time of the illness, and must indicate the impact on the student."
  )
  case object MentalHealth extends IssueType(
    description = "Mental health issue",
    helpText = "A mental health issue that is more severe than typical, short-term assessment stress and anxiety, for which you're receiving (or have asked) for support from university or other mental health services, supported by a note from your support service.",
    evidenceGuidance = "A letter from a qualified health professional (e.g. counsellor) on official, headed paper or with an official stamp. It must confirm the mental health issue with dates, and must be recorded at the time of the issue, and must indicate the impact on the student."
  )
  case object SeriousMedicalOther extends IssueType(
    description = "Serious accident or illness of someone close",
    helpText = "This would normally be a close family member, and would be supported by a doctor's note. Conditions which require you to undertake new and significant caring responsibilities are particularly relevant.",
    evidenceGuidance = "A letter from a health professional on official, headed paper or with an official stamp confirming the circumstances and how the affect your ability to do the assessment, with the dates, and some evidence of closeness. For carers, proof that you have substantial care and support responsibilities for the person."
  )
  case object Employment extends IssueType(
    description = "Significant changes in employment circumstances",
    helpText = "As a part-time student, if you’re also in employment and your employer makes changes beyond your control, eg to your working hours or your place of employment, this may count as mitigating circumstances.",
    evidenceGuidance = "A letter from from your employer confirming new working hours and/or a statement from your personal tutor or similar indicating the impact on you."
  )
  case object Deterioration extends IssueType(
    description = "Deterioration of a permanent condition",
    helpText = "A condition which you have already reported and is already covered by reasonable adjustments, but which has become significantly worse.",
    evidenceGuidance = "A letter from a qualified health professional (e.g. medical doctor, nurse) on official, headed paper or with an official stamp. It must confirm the deterioration with dates, and must be recorded at the time of the deterioration, and must indicate the impact on the student."
  )
  case object Bereavement extends IssueType(
    description = "Bereavement",
    helpText = "The death of someone close to you (normally a close family member) around the time of an assessment, supported by a death certificate or funeral notice.",
    evidenceGuidance = "A copy of the death certificate, or order of funeral service, or death announcement in a newspaper or on the web."
  )
  case object AbruptChange extends IssueType(
    description = "Sudden change in personal circumstances",
    helpText = "Changes of this sort may include a divorce or separation, a fire, a court appearance, or an acute accommodation crisis.",
    evidenceGuidance = "A letter from a doctor, solicitor or other professional person, on official headed paper confirming the circumstances, the dates, and evidence of how it affects your ability to do the assessment. For financial problems, evidence of unforeseen hardship, eg bank statements or a letter of support from Student Funding or the Hardship Fund."
  )
  case object LateDiagnosis extends IssueType(
    description = "Late diagnosis of a specific learning difference",
    helpText = "If you have not previously been diagnosed with a disability (including a specific learning difference), but receive such a diagnosis close to an assessment, this may count as mitigating circumstances.",
    evidenceGuidance = "A diagnosis letter and confirmation from your department or from Disability Services that the diagnosis was submitted too late and missed the University deadline."
  )
  case object VictimOfCrime extends IssueType(
    description = "Victim of crime",
    helpText = "If you are the victim of a crime (normally supported by a crime number provided by the police) which has caused you significant distress and/or practical difficulties, this may count as mitigating circumstances. Involvement in a criminal case as a witness may also count.",
    evidenceGuidance = "A crime reference number, and either an official police report giving the date of the crime or a letter from health professional, or Senior Tutor or similar, explaining how the circumstances are affecting your ability to do the assessment."
  )
  case object Harassment extends IssueType(
    description = "Suffered bullying, harassment, victimisation or threatening behaviour",
    helpText = "If you have suffered behaviour which has caused you significant distress and which you have reported to an appropriate body, this may count as mitigating circumstances.",
    evidenceGuidance = "A report from Senior Tutor or Student Support Services or Students’ Union Advice Centre outlining the circumstance with dates affected, and copies of relevant emails, screenshots, videos, social media posts, witness statements, or other communications (with dates where possible) that illustrate bullying, harassment or threatening behaviour."
  )
  case object IndustrialAction extends IssueType(
    description = "Industrial action",
    helpText = "If your studies are affected by industrial action (eg. your lectures or seminars get cancelled or rearranged) then this may count as mitigating circumstances. A statement of changes that have affected you, provided by your department, should normally be provided as supporting evidence.",
    evidenceGuidance = "A statement from your department of the dates of industrial action and the effect it had on teaching, marking or examinations."
  )
  case object Other extends IssueType(
    description = "Other",
    helpText = "This list is not exhaustive and if you want to report a claim for mitigating circumstances which is not listed above, but does in in your opinion represent a mitigating circumstance, you should list it here.",
    evidenceGuidance = "Please supply independent evidence from a relevant professional person or body that explains what happened (including dates) and the effect it had on you."
  )

  def validIssueTypes(student: StudentMember): Seq[IssueType] = {
    // TODO - Make it possible for TQ to enable this (we could also just manage this in code)
    val invalidTypes =
      if (student.mostSignificantCourse.latestStudentCourseYearDetails.modeOfAttendance.code == "P") Seq(IndustrialAction)
      else Seq(Employment, IndustrialAction)

    IssueType.values.filterNot(invalidTypes.contains)
  }
}

class IssueTypeUserType extends EnumSeqUserType(IssueType)
class IssueTypeConverter extends EnumTwoWayConverter(IssueType)