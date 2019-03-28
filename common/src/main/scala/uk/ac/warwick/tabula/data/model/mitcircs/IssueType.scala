package uk.ac.warwick.tabula.data.model.mitcircs

import java.sql.Types
import org.hibernate.`type`.{StandardBasicTypes, StringType}
import uk.ac.warwick.tabula.data.model.AbstractBasicUserType
import uk.ac.warwick.tabula.system.TwoWayConverter

import uk.ac.warwick.tabula.helpers.StringUtils._

sealed abstract class IssueType(val code: String, val description: String)

object IssueType {

  case object SeriousMedical extends IssueType(code = "SeriousMedical", description = "Serious accident or illness")
  case object SeriousMedicalOther extends IssueType(code = "SeriousMedicalOther", description = "Serious accident or illness of someone close")
  case object Employment extends IssueType(code = "Employment", description = "Significant changes in employment circumstances")
  case object Deterioration extends IssueType(code = "Deterioration", description = "Deterioration of a permanent condition")
  case object Bereavement extends IssueType(code = "Bereavement", description = "Bereavement")
  case object AbruptChange extends IssueType(code = "AbruptChange", description = "Abrupt change in personal circumstances")
  case object LateDiagnosis extends IssueType(code = "LateDiagnosis", description = "Late diagnosis of a specific learning difference")
  case object Harassment extends IssueType(code = "Harassment", description = "Suffered bullying, harassment, victimization or threatening behavior")
  case object Other extends IssueType(code = "Other", description = "Other")

  def values: Seq[IssueType] = Seq(SeriousMedical, SeriousMedicalOther, Employment, Deterioration, Bereavement, AbruptChange, LateDiagnosis, Harassment, Other)

  def fromCode(code: String): IssueType = values.find(_.code == code).getOrElse(throw new IllegalArgumentException())

}

class IssueTypeUserType extends AbstractBasicUserType[IssueType, String] {

  val basicType: StringType = StandardBasicTypes.STRING

  override def sqlTypes = Array(Types.VARCHAR)

  val nullValue: Null = null
  val nullObject: Null = null

  override def convertToObject(string: String): IssueType = IssueType.fromCode(string)

  override def convertToValue(issueType: IssueType): String = issueType.code
}

class IssueTypeConverter extends TwoWayConverter[String, IssueType] {
  override def convertRight(code: String): IssueType = if (code.hasText) IssueType.fromCode(code) else null

  override def convertLeft(issueType: IssueType): String = Option(issueType).map(_.code).orNull
}