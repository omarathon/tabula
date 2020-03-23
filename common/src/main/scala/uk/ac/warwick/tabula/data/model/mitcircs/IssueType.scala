package uk.ac.warwick.tabula.data.model.mitcircs

import enumeratum.{EnumEntry, _}
import uk.ac.warwick.tabula.data.model.{EnumSeqUserType, StudentMember}
import uk.ac.warwick.tabula.system.EnumTwoWayConverter

import scala.collection.immutable

sealed abstract class IssueType(val description: String, val helpText: String, val evidenceGuidance: String) extends EnumEntry

sealed abstract class CoronavirusIssueType(description: String, helpText: String, override val evidenceGuidance: String = "") extends IssueType(description, helpText, evidenceGuidance)

object IssueType extends Enum[IssueType] {

  val values: immutable.IndexedSeq[IssueType] = findValues

  case object SeriousAccident extends IssueType(
    description = "Serious accident",
    helpText = "An accident which had a significant effect on your ability to complete an assessment. Normally the accident would have required you to receive medical treatment and would be supported by a doctor's (or other healthcare professional) note.",
    evidenceGuidance = "A letter from a qualified health professional (e.g. medical doctor, nurse) on official, headed paper or with an official stamp. It must confirm the accident with dates, and must be recorded at the time of the accident, and must indicate the impact on the student."
  )
  case object SeriousPhysicalIllness extends IssueType(
    description = "Serious physical illness",
    helpText = "An illness that might require medication prescribed by a GP, or a referral to a specialist, supported by a doctor's (or other healthcare professional) note. Minor illnesses such as coughs and colds not requiring treatment would not normally be eligible.",
    evidenceGuidance = "A letter from a qualified health professional (e.g. medical doctor, nurse) on official, headed paper or with an official stamp. It must confirm the illness with dates, and must be recorded at the time of the illness, and must indicate the impact on the student."
  )
  case object MentalHealth extends IssueType(
    description = "Mental health issue",
    helpText = "A mental health issue for which you’re receiving or are waiting for support from university or other mental health services or your GP, supported by a note from your support service or GP / healthcare professional. Issues arising from short-term assessment stress and anxiety are not normally eligible unless it is a flare-up of a pre-diagnosed illness / condition.",
    evidenceGuidance = "A letter from a qualified health professional (e.g. medical doctor, counsellor) on official, headed paper or with an official stamp. It must confirm the mental health issue with dates, and must be recorded at the time of the issue, and must indicate the impact on the student."
  )
  case object SeriousMedicalOther extends IssueType(
    description = "Serious accident or illness of someone close",
    helpText = "This would normally be a close family member, and would be supported by a doctor's note. Conditions which require you to undertake new and significant caring responsibilities are particularly relevant.",
    evidenceGuidance = "A letter from a health professional on official, headed paper or with an official stamp confirming the circumstances,  with the dates, and some evidence of closeness. For carers, proof that you have substantial care and support responsibilities for the person. You should also indicate how this affected your ability to do the assessment."
  )
  case object Employment extends IssueType(
    description = "Significant changes in employment circumstances",
    helpText = "As a part-time student, if you’re also in employment and your employer makes changes beyond your control, e.g. to your working hours or your place of employment.",
    evidenceGuidance = "A letter from from your employer confirming new working hours and/or a statement from your personal tutor or similar indicating the impact on you."
  )
  case object Deterioration extends IssueType(
    description = "Deterioration of a permanent condition",
    helpText = "A condition which you have already reported and is already covered by reasonable adjustments, but which has become significantly worse.",
    evidenceGuidance = "A letter from a qualified health professional (e.g. medical doctor, nurse, mental health professional) on official, headed paper or with an official stamp. It must confirm the deterioration with dates, and must be recorded at the time of the deterioration, and must indicate the impact on the student."
  )
  case object Bereavement extends IssueType(
    description = "Bereavement",
    helpText = "The death of someone close to you (normally a close family member or close friend) around the time of an assessment, supported by a death certificate or funeral notice.",
    evidenceGuidance = "Depending upon your circumstances you may not be able to upload any evidence, (e.g order of funeral service, death announcement, death certificate).  If you do have documentation and feel able to share it, please submit it here. Alternatively, you can share information about your circumstances with your personal or senior tutor who can acknowledge your claim on your behalf (you should tick the ‘sensitive evidence’ box below in this case)."
  )
  case object AbruptChange extends IssueType(
    description = "Sudden change in personal circumstances",
    helpText = "Changes of this sort may include a divorce or separation, a sudden change in financial circumstances, a court appearance, or an acute accommodation crisis.",
    evidenceGuidance = "A letter from a doctor, solicitor or other professional person, on official headed paper confirming the circumstances, the dates, and evidence of how it affects your ability to do the assessment. For financial problems, evidence of unforeseen hardship, e.g. bank statements or a letter of support from Student Funding or the Hardship Fund."
  )
  case object LateDiagnosis extends IssueType(
    description = "Late diagnosis of a specific learning difference",
    helpText = "If you have not previously been diagnosed with a disability (including a specific learning difference), but receive such a diagnosis close to an assessment.",
    evidenceGuidance = "A diagnosis letter and confirmation from your department or from Disability Services that the diagnosis was submitted too late and missed the University deadline."
  )
  case object VictimOfCrime extends IssueType(
    description = "Victim of crime",
    helpText = "If you are the victim of a crime (normally supported by a crime number provided by the police) which has caused you significant distress and/or practical difficulties. Involvement in a criminal case as a witness may also be eligible.",
    evidenceGuidance = "A crime reference number, and either an official police report giving the date of the crime or a letter from health professional, or Senior Tutor or similar, explaining how the circumstances are affecting your ability to do the assessment."
  )
  case object Harassment extends IssueType(
    description = "Suffered bullying, harassment, victimisation or threatening behaviour",
    helpText = "If you have suffered behaviour which has caused you significant distress and which you have reported to an appropriate body.",
    evidenceGuidance = "A report from Senior Tutor or Wellbeing Support Services or Students’ Union Advice Centre outlining the circumstance with dates affected."
  )
  case object IndustrialAction extends IssueType(
    description = "Industrial action",
    helpText = "If your studies are affected by industrial action (e.g. your lectures or seminars get cancelled or rearranged) then this may be eligible as mitigating circumstances. A statement of the disruption that has occurred should be provided by your department, and you should say how this has affected your ability to complete your assessments.",
    evidenceGuidance = "A statement from your department of the disruption that has affected your studies."
  )
  case object Other extends IssueType(
    description = "Other",
    helpText = "This may include: gender transition or gender reassignment; maternity, paternity or adoption leave; caring responsibilities. However, this list is not exhaustive. If you want to report a claim for something which you believe represents a mitigating circumstance, but which is not shown on this form, you should enter it here.",
    evidenceGuidance = "Please supply independent evidence from a relevant professional person or body that explains what happened (including dates) and the effect it had on you."
  ) {
    val covidHelpText = "If coronavirus has affected your circumstances in some other way, please tick this option and tell us something about what has happened"
  }

  case object SelfIsolate extends CoronavirusIssueType(
    description = "Advised by NHS111 / medical service to self isolate",
    helpText = "If you have been advised by NHS111 or the local equivalent in your country, or by your GP or a doctor, that you need to self-isolate, tick this option",
  )

  case object SelfIsolate7Days extends CoronavirusIssueType(
    description = "Currently self-isolating for 7 days due to a persistent cough or fever or other symptoms",
    helpText = "If you’re self-isolating for 7 days because you have a persistent cough, fever, or other relevant symptoms, tick this option",
  )

  case object SelfIsolate14Days extends CoronavirusIssueType(
    description = "Currently self-isolating for 14 days",
    helpText = "If you’re self-isolating for 14 days because you have come into close contact with a person with symptoms that suggest coronavirus (even though you may not be showing symptoms yourself), tick this option",
  )

  case object Diagnosed extends CoronavirusIssueType(
    description = "Diagnosed with coronavirus and/or a coronavirus hospital inpatient",
    helpText = "If you have been diagnosed with coronavirus by a doctor, tick this option",
    evidenceGuidance = "Please provide the date you were diagnosed and/or entered hospital, the length of time you were ill or hospitalised, and the name of the hospital where you were treated. At a later date we may ask you to upload any formal evidence of your diagnosis and treatment."
  )

  case object AwaitingResults extends CoronavirusIssueType(
    description = "Awaiting the result of a coronavirus test",
    helpText = "If you have been tested for coronavirus but have not yet received the results of your test, tick this option",
    evidenceGuidance = "Please provide the test result when known."
  )

  case object CoronavirusBereavement extends CoronavirusIssueType(
    description = "Bereavement due to coronavirus",
    helpText = "If there has been a death of someone in your family or close to you as a result of coronavirus, tick this option",
    evidenceGuidance = "Depending upon your circumstances you may not be able to upload any evidence, (e.g order of funeral service, death announcement, death certificate).  If you do have documentation and feel able to share it, please submit it here. Alternatively, you can share information about your circumstances with your personal or senior tutor who can acknowledge your claim on your behalf (you should tick the ‘sensitive evidence’ box below in this case)."
  )

  case object Carer extends CoronavirusIssueType(
    description = "Carer for a coronavirus patient ",
    helpText = "If you are acting as the carer for someone (other than yourself) who is suffering from coronavirus, tick this option",
    evidenceGuidance = "Please provide the date the patient was diagnosed and/or entered hospital, the length of time they were ill or hospitalised, and the name of the hospital where they were treated. At a later date we may ask you to upload any formal evidence of the coronavirus patient in question."
  )

  case object CarerSelfIsolate extends CoronavirusIssueType(
    description = "Carer for a family/household member required to self-isolate",
    helpText = "If someone in your family has been required to self-isolate, and you need to care for them while they are isolated, tick this option",
  )

  case object CarerChildcareClosure extends CoronavirusIssueType(
    description = "Carer of children due to school closure",
    helpText = "If you are experiencing difficulties due to childcare (eg difficulty fully participating in on-line teaching or assessment), tick this box",
    evidenceGuidance = "Please tell us how this has affected your ability to study and the name of the school(s) and dates closed. "
  )

  case object NoVisa extends CoronavirusIssueType(
    description = "Not able to obtain a Visa",
    helpText = "If you have not been able to obtain a visa to come to the UK because of travel or other restrictions put in place by your country or by the UK, tick this option",
    evidenceGuidance = "Please provide us with any visa rejection. Please contact your department to find out how you can be provided with more support through on-line learning and assessment."
  )

  case object CannotTravel extends CoronavirusIssueType(
    description = "Cannot travel to the UK",
    helpText = "If there are travel restrictions in place which make it impossible for you to travel to the UK, tick this option",
    evidenceGuidance = "Please provide links to any government advice/official travel restrictions or cancelled flight tickets where available. Please contact your department to find out how you can be provided with more support through on-line learning and assessment. "
  )

  def coronavirusIssueTypes: Seq[IssueType] = (IssueType.values.collect { case i: CoronavirusIssueType => i }) ++ Seq(Other)
  def generalIssueTypes: Seq[IssueType] =  IssueType.values.diff(coronavirusIssueTypes) ++ Seq(Other)

  def validIssueTypes(student: StudentMember): Seq[IssueType] = {
    // TODO - Make it possible for TQ to enable this (we could also just manage this in code)
    val invalidTypes =
      if (Option(student.mostSignificantCourse).flatMap(scd => Option(scd.latestStudentCourseYearDetails)).flatMap(scyd => Option(scyd.modeOfAttendance)).map(_.code).contains("P")) Seq(IndustrialAction)
      else Seq(Employment, IndustrialAction)

    generalIssueTypes.filterNot(invalidTypes.contains)
  }
}

class IssueTypeUserType extends EnumSeqUserType(IssueType)
class IssueTypeConverter extends EnumTwoWayConverter(IssueType)
