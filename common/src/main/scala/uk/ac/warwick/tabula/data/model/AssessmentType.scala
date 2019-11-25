package uk.ac.warwick.tabula.data.model

import enumeratum.{Enum, EnumEntry}
import uk.ac.warwick.tabula.data.convert.ConvertibleConverter

import scala.collection.immutable

sealed abstract class TabulaAssessmentSubtype(val code: String) extends EnumEntry
object TabulaAssessmentSubtype extends Enum[TabulaAssessmentSubtype] {

  case object Assignment extends TabulaAssessmentSubtype("A")
  case object Exam extends TabulaAssessmentSubtype("E")
  case object Other extends TabulaAssessmentSubtype("O")

  override val values: immutable.IndexedSeq[TabulaAssessmentSubtype] = findValues
}

sealed abstract class AssessmentType(val astCode: String, val subtype: TabulaAssessmentSubtype) extends EnumEntry with Convertible[String] {
  val code:String = subtype.code
  override val value: String = astCode
import scala.collection.immutable

sealed abstract class TabulaAssessmentSubtype(val code: String) extends EnumEntry
object TabulaAssessmentSubtype extends Enum[TabulaAssessmentSubtype] {

  case object Assignment extends TabulaAssessmentSubtype("A")
  case object Exam extends TabulaAssessmentSubtype("E")
  case object Other extends TabulaAssessmentSubtype("O")

  override val values: immutable.IndexedSeq[TabulaAssessmentSubtype] = findValues
}

// full list of types taken from here - https://repo.elab.warwick.ac.uk/projects/MAPP/repos/app/browse/app/sits/AssessmentHelper.scala#92-156
// TODO - fetch the list via an API call to module approval rather than maintaining it twice
sealed abstract class AssessmentType(val astCode: String, val subtype: TabulaAssessmentSubtype) extends EnumEntry with Convertible[String] {
  val code:String = subtype.code
  override val value: String = astCode
}

sealed abstract class AssignmentType(astCode: String) extends AssessmentType(astCode, TabulaAssessmentSubtype.Assignment)
sealed abstract class ExamType(astCode: String) extends AssessmentType(astCode, TabulaAssessmentSubtype.Exam)

object AssessmentType extends Enum[AssessmentType] {

  implicit val factory: String => AssessmentType = { astCode: String => values.find(_.astCode == astCode).getOrElse(Other) }

  case object Other extends AssessmentType("O", TabulaAssessmentSubtype.Other)

  case object SummerExam extends ExamType("E")
  case object JanuaryExam extends ExamType("EJ")
  case object MarchExam extends ExamType("EM")
  case object AprilExam extends ExamType("EA")
  case object MayExam extends ExamType("EY")
  case object SeptemberExam extends ExamType("ES")
  case object DecemberExam extends ExamType("ED")
  case object LocalExamination extends ExamType("LX")

  case object Abstract extends AssignmentType("AB")
  case object Curation extends AssignmentType("C")
  case object CommonplaceBook extends AssignmentType("CB")
  case object CriticalReview extends AssignmentType("CR")
  case object ComputerBasedInClassTest extends AssignmentType("CTC")
  case object ComputerBasedTakeHomeTest extends AssignmentType("CTH")
  case object Dissertation extends AssignmentType("D")
  case object DesignAssignment extends AssignmentType("DA")
  case object DissertationPlan extends AssignmentType("DP")
  case object DissertationProgressReport extends AssignmentType("DPR")
  case object Essay extends AssignmentType("WRI")
  case object EssayDraft extends AssignmentType("WRID")
  case object EssayPlan extends AssignmentType("WRIP")
  case object FilmProduction extends AssignmentType("FI")
  case object Laboratory extends AssignmentType("LAB")
  case object LiteratureReview extends AssignmentType("LR")
  case object ObjectiveStructuredClinicalAssessment extends AssignmentType("OSCA")
  case object PolicyBrief extends AssignmentType("PB")
  case object Performance extends AssignmentType("PERF")
  case object GroupProjectMarkedCollectively extends AssignmentType("PJG")
  case object GroupProjectMarkedIndividually extends AssignmentType("PJGI")
  case object GroupProjectCombinedIndividualAndCollectiveMark extends AssignmentType("PJGIC")
  case object IndividualProject extends AssignmentType("PJI")
  case object WorkBasedLearningOrPlacement extends AssignmentType("PL")
  case object GroupPosterPresentationMarkedCollectively extends AssignmentType("POG")
  case object GroupPosterPresentationMarkedIndividually extends AssignmentType("POGI")
  case object GroupPosterPresentationCombinedIndividualAndCollectiveMark extends AssignmentType("POGIC")
  case object IndividualPosterPresentation extends AssignmentType("POI")
  case object PortfolioAssignment extends AssignmentType("PORT")
  case object ProblemSet extends AssignmentType("PR")
  case object GroupPresentationMarkedCollectively extends AssignmentType("PRG")
  case object GroupPresentationCombinedIndividualAndCollectiveMark extends AssignmentType("PRGIC")
  case object GroupPresentationMarkedIndividually extends AssignmentType("PRGI")
  case object IndividualPresentation extends AssignmentType("PRI")
  case object OnlinePublication extends AssignmentType("PUB")
  case object ReflectivePiece extends AssignmentType("REF")
  case object WrittenReport extends AssignmentType("REP")
  case object StudentDevisedAssessment extends AssignmentType("SDA")
  case object SystemsModellingAndSystemIdentificationAssignment extends AssignmentType("SM")
  case object MootTrial extends AssignmentType("TR")
  case object Workshop extends AssignmentType("W")
  case object Worksheet extends AssignmentType("WS")
  case object Oral extends AssignmentType("OE")
  case object TakeHome extends AssignmentType("HE")
  case object InClassTestMultipleChoice extends AssignmentType("TMC")
  case object InClassTestShortAnswer extends AssignmentType("TSA")
  case object InClassTestOther extends AssignmentType("TO")
  case object ContributionInLearningActivities extends AssignmentType("LA")

  override val values: immutable.IndexedSeq[AssessmentType] = findValues
}

class AssessmentTypeUserType extends ConvertibleStringUserType[AssessmentType]
class AssessmentTypeConverter extends ConvertibleConverter[String, AssessmentType]