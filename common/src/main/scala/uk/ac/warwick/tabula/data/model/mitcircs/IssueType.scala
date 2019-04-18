package uk.ac.warwick.tabula.data.model.mitcircs


import enumeratum.EnumEntry
import enumeratum._
import uk.ac.warwick.tabula.data.model.EnumSeqUserType

import scala.collection.immutable
import uk.ac.warwick.tabula.system.EnumSeqTwoWayConverter

sealed abstract class IssueType(val description: String) extends EnumEntry

sealed abstract class SeriousMedicalIssue(description: String) extends IssueType(description)

object IssueType extends Enum[IssueType] {

  val values: immutable.IndexedSeq[IssueType] = findValues

  case object Accident extends SeriousMedicalIssue(description = "Accident")
  case object PhysicalInjury extends SeriousMedicalIssue(description = "Physical injury")
  case object MentalHealth extends SeriousMedicalIssue(description = "Mental health issue")

  case object SeriousMedicalOther extends IssueType(description = "Serious accident or illness of someone close")
  case object Employment extends IssueType(description = "Significant changes in employment circumstances")
  case object Deterioration extends IssueType(description = "Deterioration of a permanent condition")
  case object Bereavement extends IssueType(description = "Bereavement")
  case object AbruptChange extends IssueType(description = "Abrupt change in personal circumstances")
  case object LateDiagnosis extends IssueType(description = "Late diagnosis of a specific learning difference")
  case object Harassment extends IssueType(description = "Suffered bullying, harassment, victimization or threatening behavior")
  case object IndustrialAction extends IssueType(description = "Industrial action")
  case object Other extends IssueType(description = "Other")
}

class IssueTypeUserType extends EnumSeqUserType[IssueType]
class IssueTypeConverter extends EnumSeqTwoWayConverter[IssueType]