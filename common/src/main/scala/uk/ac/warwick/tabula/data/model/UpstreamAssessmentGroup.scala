package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.apache.commons.lang3.builder.EqualsBuilder
import org.hibernate.annotations.{BatchSize, Proxy, Type}
import org.joda.time.LocalDate
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

/**
  * An assessment group is basically the smallest group of people
  * who would be submitting to the same assignment. It is identified
  * by four pieces of information:
  *
  * - The module it's in
  * - The academic year
  * - The assessment group code (1 or more assignments in the module will share this code)
  * - The occurrence (Splits a module up into multiple occurrences/cohorts per year - 99% of the time
  * there's only one value but sometimes can be 2 or even 26 in one case)
  */
@Entity
@Proxy
class UpstreamAssessmentGroup extends GeneratedId {

  // Long-form module code with hyphen and CATS value
  var moduleCode: String = _
  var assessmentGroup: String = _
  var occurrence: String = _
  var sequence: String = _

  @Basic
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Column(nullable = false)
  var academicYear: AcademicYear = _

  @ManyToOne(fetch = FetchType.EAGER, optional = true)
  @JoinColumns(value = Array(
    new JoinColumn(name = "moduleCode", referencedColumnName="moduleCode", insertable = false, updatable = false),
    new JoinColumn(name = "sequence", referencedColumnName="sequence", insertable = false, updatable = false)
  ))
  private var _assessmentComponent: AssessmentComponent = _
  def assessmentComponent: Option[AssessmentComponent] = Option(_assessmentComponent)
  def assessmentComponent_=(assessmentComponent: AssessmentComponent): Unit = _assessmentComponent = assessmentComponent

  @Column(name = "deadline")
  private var _deadline: LocalDate = _
  def deadline: Option[LocalDate] = Option(_deadline)
  def deadline_=(deadline: Option[LocalDate]): Unit = _deadline = deadline.orNull

  @OneToMany(mappedBy = "upstreamAssessmentGroup", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @BatchSize(size = 200)
  var members: JList[UpstreamAssessmentGroupMember] = JArrayList()

  def membersIncludes(user: User): Boolean = members.asScala.map(_.universityId).contains(user.getWarwickId)

  def membersIncludes(universityId: String): Boolean = members.asScala.map(_.universityId).contains(universityId)

  def replaceMembers(universityIds: Seq[String]): Unit = {
    members.clear()
    members.addAll(universityIds.distinct.map(universityId => new UpstreamAssessmentGroupMember(this, universityId)).asJava)
  }

  def isEquivalentTo(other: UpstreamAssessmentGroup): Boolean =
    new EqualsBuilder()
      .append(moduleCode, other.moduleCode)
      .append(assessmentGroup, other.assessmentGroup)
      .append(occurrence, other.occurrence)
      .append(academicYear, other.academicYear)
      .append(sequence, other.sequence)
      // This intentionally doesn't take into account deadline
      .isEquals

  override def toString: String = "%s %s g:%s o:%s s:%s d:%s" format(moduleCode, academicYear, assessmentGroup, occurrence, sequence, deadline.map(_.toString("yyyy-MM-dd")).orNull)

}

@Entity
@Proxy
class UpstreamAssessmentGroupMember extends GeneratedId with Ordered[UpstreamAssessmentGroupMember]
  with UpstreamAssessmentGroupMemberProperties {

  def this(upstreamAssessmentGroup: UpstreamAssessmentGroup, universityId: String) = {
    this()
    this.upstreamAssessmentGroup = upstreamAssessmentGroup
    this.universityId = universityId
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id")
  var upstreamAssessmentGroup: UpstreamAssessmentGroup = _

  @Column(name = "universityId")
  var universityId: String = _

  override def compare(that: UpstreamAssessmentGroupMember): Int =
    position.getOrElse(Int.MaxValue) - that.position.getOrElse(Int.MaxValue) match {
      case 0 => Ordering.String.compare(this.universityId, that.universityId)
      case nonZero => nonZero
    }

  // FIXME - just inherits from the components deadline - leaving this here in case we need to do something fancy to support resits
  def deadline: Option[LocalDate] = upstreamAssessmentGroup.deadline

  def isAgreedMark: Boolean = firstAgreedMark.isDefined

  def isResitMark: Boolean = firstResitMark.isDefined

  def firstDefinedMark: Option[Int] = firstResitMark.orElse(firstOriginalMark)

  // doesn't include resit marks
  def firstOriginalMark: Option[Int] = agreedMark.orElse(actualMark)

  // only includes resit marks
  def firstResitMark: Option[Int] = resitAgreedMark.orElse(resitActualMark)

  // only includes board agreed marks
  def firstAgreedMark: Option[Int] = resitAgreedMark.orElse(agreedMark)

  def isAgreedGrade: Boolean = firstAgreedGrade.isDefined

  def isResitGrade: Boolean = firstResitGrade.isDefined

  def firstDefinedGrade: Option[String] = firstResitGrade.orElse(firstOriginalGrade)

  // doesn't include resit grades
  def firstOriginalGrade: Option[String] = agreedGrade.orElse(actualGrade)

  // only includes resit grades
  def firstResitGrade: Option[String] = resitAgreedGrade.orElse(resitActualGrade)

  // only includes board agreed marks
  def firstAgreedGrade: Option[String] = resitAgreedGrade.orElse(agreedGrade)
}

trait UpstreamAssessmentGroupMemberProperties {

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var position: Option[Int] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var actualMark: Option[Int] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
  var actualGrade: Option[String] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var agreedMark: Option[Int] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
  var agreedGrade: Option[String] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var resitActualMark: Option[Int] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
  var resitActualGrade: Option[String] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var resitAgreedMark: Option[Int] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
  var resitAgreedGrade: Option[String] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionBooleanUserType")
  var resitExpected:  Option[Boolean] = None
}

/** currentMembers are all members excluding PWD)  **/
case class UpstreamAssessmentGroupInfo(upstreamAssessmentGroup: UpstreamAssessmentGroup, currentMembers: Seq[UpstreamAssessmentGroupMember]) {
  lazy val allMembers: Seq[UpstreamAssessmentGroupMember] = upstreamAssessmentGroup.members.asScala.toSeq
  lazy val resitMembers: Seq[UpstreamAssessmentGroupMember] = currentMembers.filter(_.resitExpected.exists(identity))
}
