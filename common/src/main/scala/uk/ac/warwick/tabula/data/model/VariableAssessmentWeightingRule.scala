package uk.ac.warwick.tabula.data.model

import javax.persistence.{Access, AccessType, Column, Entity}
import org.hibernate.annotations.{Proxy, Type}
import uk.ac.warwick.tabula.ToString

/**
 * @see https://www.mysits.com/mysits/sits990/990manuals/cams/05asspro/08prereq/09vaw.htm#vaw
 */
@Entity
@Proxy
@Access(AccessType.FIELD)
class VariableAssessmentWeightingRule extends GeneratedId with ToString {

  // Long-form module code with hyphen and CATS value
  @Column(name = "module_code", nullable = false)
  var moduleCode: String = _

  @Column(name = "assessment_group", nullable = false)
  var assessmentGroup: String = _

  @Column(name = "rule_sequence", nullable = false)
  var ruleSequence: String = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.AssessmentTypeUserType")
  @Column(name = "assessment_type", nullable = false)
  var assessmentType: AssessmentType = _

  @Column(nullable = false)
  var weighting: Int = _

  def matchesKey(other: VariableAssessmentWeightingRule): Boolean =
    moduleCode == other.moduleCode &&
    assessmentGroup == other.assessmentGroup &&
    ruleSequence == other.ruleSequence

  def copyFrom(other: VariableAssessmentWeightingRule): Unit = {
    require(matchesKey(other))

    assessmentType = other.assessmentType
    weighting = other.weighting
  }

  override def toStringProps: Seq[(String, Any)] = Seq(
    "moduleCode" -> moduleCode,
    "assessmentGroup" -> assessmentGroup,
    "ruleSequence" -> ruleSequence,
    "assessmentType" -> assessmentType,
    "weighting" -> weighting
  )
}
