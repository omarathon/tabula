package uk.ac.warwick.tabula.data.model

import javax.persistence.CascadeType.ALL
import javax.persistence._
import org.hibernate.annotations.{BatchSize, Filter, FilterDef, Proxy, Type}
import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports.{JList, _}
import uk.ac.warwick.tabula.helpers.RequestLevelCache
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.AssessmentMembershipService
import uk.ac.warwick.tabula.{AcademicYear, ToString}

import scala.jdk.CollectionConverters._

@FilterDef(name = Assignment.NotDeletedFilter, defaultCondition = "deleted = false")
@Filter(name = Assignment.NotDeletedFilter)
@Entity
@Proxy
@Access(AccessType.FIELD)
class Exam extends GeneratedId with CanBeDeleted with PermissionsTarget with StringId with Serializable with ToString {

  @transient
  var assessmentMembershipService: AssessmentMembershipService = Wire[AssessmentMembershipService]("assignmentMembershipService")

  def this(name: String, department: Department, academicYear: AcademicYear) = {
    this()
    this.name = name
    this.department = department
    this.academicYear = academicYear
  }

  @Column(nullable = false)
  var name: String = _

  @Column(name = "startDateTime")
  private var _startDateTime: DateTime = _
  def startDateTime: Option[DateTime] = Option(_startDateTime)
  def startDateTime_=(date: DateTime): Unit = _startDateTime = date

  @ManyToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id")
  var department: Department = _

  @OneToMany(mappedBy = "exam", fetch = FetchType.LAZY, cascade = Array(ALL))
  @BatchSize(size = 200)
  var questions: JList[ExamQuestion] = JArrayList()

  @OneToMany(mappedBy = "exam", fetch = FetchType.LAZY, cascade = Array(ALL))
  @BatchSize(size = 200)
  var rules: JList[ExamQuestionRule] = JArrayList()

  @Basic
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Column(nullable = false)
  var academicYear: AcademicYear = AcademicYear.now()

  @Column(nullable = false)
  var lastModified: DateTime = DateTime.now()

  @OneToMany(mappedBy = "exam", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @BatchSize(size = 200)
  var assessmentGroups: JList[AssessmentGroup] = JArrayList()

  // converts the assessmentGroups to UpstreamAssessmentGroupInfo
  def upstreamAssessmentGroupInfos: Seq[UpstreamAssessmentGroupInfo] = RequestLevelCache.cachedBy("Exam.upstreamAssessmentGroupInfos", id) {
    assessmentMembershipService.getUpstreamAssessmentGroupInfo(assessmentGroups.asScala.toSeq, academicYear)
  }

  override def toStringProps: Seq[(String, Any)] = Seq(
    "id" -> id,
    "department" -> department.id
  )

  override def permissionsParents: LazyList[PermissionsTarget] = LazyList(department)
}
