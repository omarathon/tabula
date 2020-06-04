package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.apache.commons.lang3.builder.CompareToBuilder
import org.hibernate.annotations.{BatchSize, JoinColumnOrFormula, JoinColumnsOrFormulas, JoinFormula, Proxy, Type}
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.RequestLevelCache
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.AssessmentMembershipService
import uk.ac.warwick.tabula.system.permissions.Restricted
import uk.ac.warwick.tabula.{AcademicYear, SprCode}
import uk.ac.warwick.util.termdates.AcademicYearPeriod.PeriodType

import scala.jdk.CollectionConverters._
import scala.util.Try

object ModuleRegistration {
  final val GraduationBenchmarkCutoff: LocalDate = AcademicYear(2019).termOrVacation(PeriodType.springTerm).lastDay

  // a list of all the markscheme codes that we consider to be pass/fail modules
  final val PassFailMarkSchemeCodes = Seq("PF")
}

/*
 * sprCode, moduleCode, cat score, academicYear and occurrence are a notional key for this table but giving it a generated ID to be
 * consistent with the other tables in Tabula which all have a key that's a single field.  In the db, there should be
 * a unique constraint on the combination of those three.
 */
@Entity
@Proxy
@Access(AccessType.FIELD)
class ModuleRegistration extends GeneratedId with PermissionsTarget with CanBeDeleted with Ordered[ModuleRegistration] with Serializable {

  def this(sprCode: String, module: Module, cats: JBigDecimal, academicYear: AcademicYear, occurrence: String, marksCode: String) {
    this()
    this.sprCode = sprCode
    this.module = module
    this.academicYear = academicYear
    this.cats = cats
    this.occurrence = occurrence
    this.marksCode = marksCode.maybeText.orNull
  }

  @transient var membershipService: AssessmentMembershipService = Wire[AssessmentMembershipService]

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "moduleCode", referencedColumnName = "code")
  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var module: Module = _

  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var cats: JBigDecimal = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var academicYear: AcademicYear = _

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "StudentCourseDetails_ModuleRegistration",
    joinColumns = Array(new JoinColumn(name = "module_registration_id", insertable = false, updatable = false)),
    inverseJoinColumns = Array(new JoinColumn(name = "scjcode", insertable = false, updatable = false))
  )
  @JoinColumn(name = "scjcode", insertable = false, updatable = false)
  @BatchSize(size = 200)
  var _allStudentCourseDetails: JSet[StudentCourseDetails] = JHashSet()

  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  def studentCourseDetails: StudentCourseDetails =
    _allStudentCourseDetails.asScala.find(_.mostSignificant)
      .orElse(_allStudentCourseDetails.asScala.maxByOption(_.scjCode))
      .orNull

  // I'd love for this to be _sprCode but Hibernate won't let you mix logical and physical column names (even with @Column)
  // and it's needed for the @JoinColumn on StudentCourseDetails._moduleRegistrations
  var sprCode: String = _

  // get the integer part of the SPR code so we can sort registrations to the same module by it
  def sprSequence: Int = sprCode.split("/").lastOption.flatMap(s => Try(s.toInt).toOption).getOrElse(Int.MinValue)

  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var assessmentGroup: String = _

  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var occurrence: String = _

  @Restricted(Array("Profiles.Read.ModuleRegistration.Results"))
  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var actualMark: Option[Int] = None

  @Restricted(Array("Profiles.Read.ModuleRegistration.Results"))
  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
  var actualGrade: Option[String] = None

  @Restricted(Array("Profiles.Read.ModuleRegistration.Results"))
  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var agreedMark: Option[Int] = None

  @Restricted(Array("Profiles.Read.ModuleRegistration.Results"))
  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
  var agreedGrade: Option[String] = None

  def firstDefinedMark: Option[Int] = agreedMark.orElse(actualMark)

  def firstDefinedGrade: Option[String] = agreedGrade.orElse(actualGrade)

  def hasAgreedMarkOrGrade: Boolean = agreedMark.isDefined || agreedGrade.isDefined

  @Type(`type` = "uk.ac.warwick.tabula.data.model.ModuleSelectionStatusUserType")
  @Column(name = "selectionstatuscode")
  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var selectionStatus: ModuleSelectionStatus = _ // core, option or optional core

  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var lastUpdatedDate: DateTime = DateTime.now

  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var marksCode: String = _

  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  def passFail: Boolean = marksCode.maybeText.exists(ModuleRegistration.PassFailMarkSchemeCodes.contains)

  @Type(`type` = "uk.ac.warwick.tabula.data.model.ModuleResultUserType")
  @Column(name = "moduleresult")
  var moduleResult: ModuleResult = _

  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var endDate: LocalDate = _

  def passedCats: Option[Boolean] = moduleResult match {
    case _: ModuleResult.Pass.type if hasAgreedMarkOrGrade => Some(true)
    case _: ModuleResult.Fail.type if hasAgreedMarkOrGrade => Some(false)
    case _ => None
  }

  def upstreamAssessmentGroups: Seq[UpstreamAssessmentGroup] =
    RequestLevelCache.cachedBy("ModuleRegistration.upstreamAssessmentGroups", s"$academicYear-$toSITSCode-$assessmentGroup-$occurrence") {
      membershipService.getUpstreamAssessmentGroups(this, eagerLoad = false)
    }

  def upstreamAssessmentGroupMembers: Seq[UpstreamAssessmentGroupMember] =
    RequestLevelCache.cachedBy("ModuleRegistration.upstreamAssessmentGroupMembers", s"$academicYear-$toSITSCode-$assessmentGroup-$occurrence") {
      membershipService.getUpstreamAssessmentGroups(this, eagerLoad = true).flatMap(_.members.asScala)
    }.filter(_.universityId == studentCourseDetails.student.universityId)

  def currentUpstreamAssessmentGroupMembers: Seq[UpstreamAssessmentGroupMember] = {
    val withdrawnCourse = Option(studentCourseDetails.statusOnCourse).exists(_.code.startsWith("P"))
    upstreamAssessmentGroupMembers.filterNot(_ => withdrawnCourse)
  }

  def componentsForBenchmark: Seq[UpstreamAssessmentGroupMember] = {
    upstreamAssessmentGroupMembers
      .filter(_.deadline.exists(d => d.isBefore(ModuleRegistration.GraduationBenchmarkCutoff) || d.isEqual(ModuleRegistration.GraduationBenchmarkCutoff)))
      .filter(_.firstDefinedMark.isDefined)
  }

  def componentMarks(includeActualMarks: Boolean): Seq[(AssessmentType, String, Option[Int])] =
    upstreamAssessmentGroupMembers.flatMap { uagm =>
      uagm.upstreamAssessmentGroup.assessmentComponent.map { ac =>
        (ac.assessmentType, ac.sequence, if (includeActualMarks) uagm.firstDefinedMark else uagm.firstAgreedMark)
      }
    }

  override def toString: String = s"$sprCode-${module.code}-$cats-$academicYear"

  //allowing module manager to see MR records - TAB-6062(module grids)
  def permissionsParents: LazyList[PermissionsTarget] = LazyList(Option(studentCourseDetails), Option(module)).flatten

  override def compare(that: ModuleRegistration): Int =
    new CompareToBuilder()
      .append(sprCode, that.sprCode)
      .append(module, that.module)
      .append(cats, that.cats)
      .append(academicYear, that.academicYear)
      .append(occurrence, that.occurrence)
      .build()

  def toSITSCode: String = "%s-%s".format(module.code.toUpperCase, cats.stripTrailingZeros().toPlainString)

  def moduleList(route: Route): Option[UpstreamModuleList] = route.upstreamModuleLists.asScala
    .filter(_.academicYear == academicYear)
    .find(_.matches(toSITSCode))
}

/**
  * Holds data about an individual student's registration on a single module.
  */
case class UpstreamModuleRegistration(
  year: String,
  sprCode: String,
  seatNumber: String,
  occurrence: String,
  sequence: String,
  moduleCode: String,
  assessmentGroup: String,
  actualMark: String,
  actualGrade: String,
  agreedMark: String,
  agreedGrade: String,
  resitActualMark: String,
  resitActualGrade: String,
  resitAgreedMark: String,
  resitAgreedGrade: String,
  resitExpected: Boolean
) {

  def universityId: String = SprCode.getUniversityId(sprCode)

  // Assessment group membership doesn't vary by sequence - for groups that are null we want to return same group -TAB-5615
  def differentGroup(other: UpstreamModuleRegistration): Boolean =
    year != other.year ||
    (occurrence != other.occurrence && assessmentGroup != AssessmentComponent.NoneAssessmentGroup) ||
    moduleCode != other.moduleCode ||
    assessmentGroup != other.assessmentGroup

  /**
    * Returns UpstreamAssessmentGroups matching the group attributes.
    */
  def toUpstreamAssessmentGroups(sequences: Seq[String]): Seq[UpstreamAssessmentGroup] = {
    sequences.map(sequence => {
      val g = new UpstreamAssessmentGroup
      g.academicYear = AcademicYear.parse(year)
      g.moduleCode = moduleCode
      g.assessmentGroup = assessmentGroup
      g.sequence = sequence
      // for the NONE group, override occurrence to also be NONE, because we create a single UpstreamAssessmentGroup
      // for each module with group=NONE and occurrence=NONE, and all unallocated students go in there together.
      g.occurrence =
        if (assessmentGroup == AssessmentComponent.NoneAssessmentGroup)
          AssessmentComponent.NoneAssessmentGroup
        else
          occurrence
      g
    })
  }

  /**
    * Returns an UpstreamAssessmentGroup matching the group attributes, including sequence.
    */
  def toExactUpstreamAssessmentGroup: UpstreamAssessmentGroup = {
    val g = new UpstreamAssessmentGroup
    g.academicYear = AcademicYear.parse(year)
    g.moduleCode = moduleCode
    g.assessmentGroup = assessmentGroup
    g.sequence = sequence
    // for the NONE group, override occurrence to also be NONE, because we create a single UpstreamAssessmentGroup
    // for each module with group=NONE and occurrence=NONE, and all unallocated students go in there together.
    g.occurrence =
      if (assessmentGroup == AssessmentComponent.NoneAssessmentGroup)
        AssessmentComponent.NoneAssessmentGroup
      else
        occurrence
    g
  }
}
