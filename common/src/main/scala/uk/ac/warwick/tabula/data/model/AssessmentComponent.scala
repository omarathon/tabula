package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.hibernate.annotations.{Proxy, Type}
import org.joda.time.Duration
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.PreSaveBehaviour
import uk.ac.warwick.tabula.services.AssessmentMembershipService
import uk.ac.warwick.tabula.{AcademicYear, ToString}

import scala.jdk.CollectionConverters._
import scala.math.BigDecimal.RoundingMode

/**
  * Represents an upstream assessment component as found in the central
  * University systems. An component is timeless - it doesn't
  * relate to a specific instance of an assignment/exam or even a particular year.
  *
  */
@Entity
@Proxy
class AssessmentComponent extends GeneratedId with PreSaveBehaviour with Serializable with ToString {

  @transient var membershipService: AssessmentMembershipService = Wire.auto[AssessmentMembershipService]

  /**
    * Uppercase module code, with CATS. e.g. IN304-15
    */
  var moduleCode: String = _

  /**
    * An OPTIONAL link to a Tabula representation of a Module.
    */
  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "module_id")
  var module: Module = _

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "assessmentComponent")
  var links: JList[AssessmentGroup] = JArrayList()

  /**
    * Assessment group the assignment is in. Is mostly a meaningless
    * character but will map to a corresponding student module registratation
    * to the same group.
    */
  var assessmentGroup: String = _

  /**
    * Identifier for the assignment, unique within a given moduleCode.
    */
  var sequence: String = _

  /**
    * Name as defined upstream.
    */
  var name: String = _

  @Column(name = "IN_USE")
  var inUse: Boolean = _

  /**
    * The type of component. Typical values are A for assignment,
    * E for summer exam. Other values exist.
    */
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AssessmentTypeUserType")
  @Column(nullable = false)
  var assessmentType: AssessmentType = _

  var marksCode: String = _

  /**
   * The raw weighting of the assessment component. Beware! This may need scaling (e.g. where weightings are 125/125/750 because SITS doesn't support decimals)
   * or may be affected by VariableAssessmentWeightingRules.
   */
  @Column(name = "weighting")
  var rawWeighting: JInteger = _

  @transient lazy val scaledWeighting: Option[BigDecimal] = Option(rawWeighting).map(_.toInt).map { weighting =>
    // If VAW applies, just use the raw weighting
    if (membershipService.getVariableAssessmentWeightingRules(moduleCode, assessmentGroup).nonEmpty) BigDecimal(weighting)
    else {
      val totalWeight =
        membershipService.getAssessmentComponents(moduleCode, inUseOnly = false)
          .filter(_.assessmentGroup == assessmentGroup)
          .flatMap(ac => Option(ac.rawWeighting).map(_.toInt))
          .sum

      if (totalWeight == 100) BigDecimal(weighting)
      else {
        val bd = BigDecimal(weighting * 100) / BigDecimal(totalWeight)
        bd.setScale(1, RoundingMode.HALF_UP)
        bd
      }
    }
  }

  @Column(name = "exam_paper_code")
  private var _examPaperCode: String = _
  def examPaperCode: Option[String] = Option(_examPaperCode)
  def examPaperCode_=(code: Option[String]): Unit = _examPaperCode = code.orNull

  @Column(name = "exam_paper_title")
  private var _examPaperTitle: String = _
  def examPaperTitle: Option[String] = Option(_examPaperTitle)
  def examPaperTitle_=(title: Option[String]): Unit = _examPaperTitle = title.orNull

  @Column(name = "exam_paper_section")
  private var _examPaperSection: String = _
  def examPaperSection: Option[String] = Option(_examPaperSection)
  def examPaperSection_=(section: Option[String]): Unit = _examPaperSection = section.orNull

  @Type(`type` = "org.jadira.usertype.dateandtime.joda.PersistentDurationAsString")
  @Column(name = "exam_paper_duration")
  private var _examPaperDuration: Duration = _
  def examPaperDuration: Option[Duration] = Option(_examPaperDuration)
  def examPaperDuration_=(duration: Option[Duration]): Unit = _examPaperDuration = duration.orNull

  @Type(`type` = "org.jadira.usertype.dateandtime.joda.PersistentDurationAsString")
  @Column(name = "exam_paper_reading_time")
  private var _examPaperReadingTime: Duration = _
  def examPaperReadingTime: Option[Duration] = Option(_examPaperReadingTime)
  def examPaperReadingTime_=(readingTime: Option[Duration]): Unit = _examPaperReadingTime = readingTime.orNull

  @Type(`type` = "uk.ac.warwick.tabula.data.model.ExaminationTypeUserType")
  @Column(name = "exam_paper_type")
  private var _examPaperType: ExaminationType = _
  def examPaperType: Option[ExaminationType] = Option(_examPaperType)
  def examPaperType_=(examPaperType: Option[ExaminationType]): Unit = _examPaperType = examPaperType.orNull

  /**
    * Returns moduleCode without CATS. e.g. in304
    */
  def moduleCodeBasic: String = Module.stripCats(moduleCode).getOrElse(throw new IllegalArgumentException(s"$moduleCode did not fit expected module code pattern"))

  /**
    * Returns the CATS as a string if it's present, e.g. 50
    */
  def cats: Option[String] = Module.extractCats(moduleCode)

  def sameKey(other: AssessmentComponent): Boolean = {
    this.sequence == other.sequence && this.moduleCode == other.moduleCode
  }

  def needsUpdatingFrom(other: AssessmentComponent): Boolean =
    this.name != other.name ||
    this.module != other.module ||
    this.assessmentGroup != other.assessmentGroup ||
    this.assessmentType != other.assessmentType ||
    this.inUse != other.inUse ||
    this.marksCode != other.marksCode ||
    this.rawWeighting != other.rawWeighting ||
    this.examPaperCode != other.examPaperCode ||
    this.examPaperTitle != other.examPaperTitle ||
    this.examPaperSection != other.examPaperSection ||
    this.examPaperDuration != other.examPaperDuration ||
    this.examPaperReadingTime != other.examPaperReadingTime ||
    this.examPaperType != other.examPaperType

  override def preSave(newRecord: Boolean): Unit = {
    ensureNotNull("name", name)
    ensureNotNull("moduleCode", moduleCode)
  }

  private def ensureNotNull(name: String, value: Any): Unit = {
    if (value == null) throw new IllegalStateException("null " + name + " not allowed")
  }

  def copyFrom(other: AssessmentComponent): Unit = {
    moduleCode = other.moduleCode
    assessmentGroup = other.assessmentGroup
    sequence = other.sequence
    inUse = other.inUse
    module = other.module
    name = other.name
    assessmentType = other.assessmentType
    marksCode = other.marksCode
    rawWeighting = other.rawWeighting
    examPaperCode = other.examPaperCode
    examPaperTitle = other.examPaperTitle
    examPaperSection = other.examPaperSection
    examPaperDuration = other.examPaperDuration
    examPaperReadingTime = other.examPaperReadingTime
    examPaperType = other.examPaperType
  }

  override def toStringProps: Seq[(String, Any)] = Seq(
     "moduleCode" -> moduleCode,
     "assessmentGroup" -> assessmentGroup,
     "sequence" -> sequence,
     "inUse" -> inUse,
     "module" -> module,
     "name" -> name,
     "assessmentType" -> assessmentType,
     "marksCode" -> marksCode,
     "rawWeighting" -> rawWeighting,
     "examPaperCode" -> examPaperCode,
     "examPaperTitle" -> examPaperTitle,
     "examPaperSection" -> examPaperSection,
     "examPaperDuration" -> examPaperDuration,
     "examPaperReadingTime" -> examPaperReadingTime,
     "examPaperType" -> examPaperType
  )

  def upstreamAssessmentGroups(year: AcademicYear): Seq[UpstreamAssessmentGroup] = membershipService.getUpstreamAssessmentGroups(this, year)

  def linkedAssignments: Seq[Assignment] = links.asScala.toSeq.collect { case l if l.assignment != null => l.assignment }

  def scheduledExams(academicYear: Option[AcademicYear]): Seq[AssessmentComponentExamSchedule] = membershipService.findScheduledExams(this, academicYear)
}

object AssessmentComponent {
  /** The value we store as the assessment group when no group has been chosen for this student
    * (in ADS it is null)
    *
    * We also use this value for a few other fields, like Occurrence, so the variable
    * name is a bit too specific.
    */
  val NoneAssessmentGroup = "NONE"
}
