package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.apache.commons.lang3.builder.CompareToBuilder
import org.hibernate.annotations.{Proxy, Type}
import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports.JBigDecimal
import uk.ac.warwick.tabula.helpers.RequestLevelCache
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.AssessmentMembershipService
import uk.ac.warwick.tabula.system.permissions._
import uk.ac.warwick.tabula.{AcademicYear, SprCode}

import scala.collection.JavaConverters._

/*
 * sprCode, moduleCode, cat score, academicYear and occurrence are a notional key for this table but giving it a generated ID to be
 * consistent with the other tables in Tabula which all have a key that's a single field.  In the db, there should be
 * a unique constraint on the combination of those three.
 */
@Entity
@Proxy
@Access(AccessType.FIELD)
class ModuleRegistration() extends GeneratedId with PermissionsTarget with CanBeDeleted with Ordered[ModuleRegistration] {

  def this(studentCourseDetails: StudentCourseDetails, module: Module, cats: java.math.BigDecimal, academicYear: AcademicYear, occurrence: String, passFail: Boolean = false) {
    this()
    this.studentCourseDetails = studentCourseDetails
    this.module = module
    this.academicYear = academicYear
    this.cats = cats
    this.occurrence = occurrence
    this.passFail = passFail
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "scjCode", referencedColumnName = "scjCode")
  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var studentCourseDetails: StudentCourseDetails = _

  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var assessmentGroup: String = _

  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var occurrence: String = _

  @Restricted(Array("Profiles.Read.ModuleRegistration.Results"))
  var actualMark: JBigDecimal = _

  @Restricted(Array("Profiles.Read.ModuleRegistration.Results"))
  var actualGrade: String = _

  @Restricted(Array("Profiles.Read.ModuleRegistration.Results"))
  var agreedMark: JBigDecimal = _

  @Restricted(Array("Profiles.Read.ModuleRegistration.Results"))
  var agreedGrade: String = _

  def firstDefinedMark: Option[JBigDecimal] = Option(agreedMark).orElse(Option(actualMark))

  def firstDefinedGrade: Option[String] = Option(agreedGrade).orElse(Option(actualGrade))

  def hasAgreedMarkOrGrade: Boolean = Option(agreedMark).isDefined || Option(agreedGrade).isDefined

  @Type(`type` = "uk.ac.warwick.tabula.data.model.ModuleSelectionStatusUserType")
  @Column(name = "selectionstatuscode")
  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var selectionStatus: ModuleSelectionStatus = _ // core, option or optional core

  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var lastUpdatedDate: DateTime = DateTime.now

  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var passFail: Boolean = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.ModuleResultUserType")
  @Column(name = "moduleresult")
  var moduleResult: ModuleResult = _

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

  override def toString: String = s"${studentCourseDetails.scjCode}-${module.code}-$cats-$academicYear"

  //allowing module manager to see MR records - TAB-6062(module grids)
  def permissionsParents: Stream[PermissionsTarget] = Stream(Option(studentCourseDetails), Option(module)).flatten

  override def compare(that: ModuleRegistration): Int =
    new CompareToBuilder()
      .append(studentCourseDetails, that.studentCourseDetails)
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
  resitAgreedGrade: String
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
