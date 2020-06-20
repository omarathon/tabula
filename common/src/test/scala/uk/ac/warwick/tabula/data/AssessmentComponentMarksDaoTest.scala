package uk.ac.warwick.tabula.data

import org.junit.Before
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, AssessmentType, Department, Module, RecordedAssessmentComponentStudent, UpstreamAssessmentGroup, UpstreamAssessmentGroupMember, UpstreamAssessmentGroupMemberAssessmentType}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, PersistenceTestBase}
import uk.ac.warwick.tabula.JavaImports._

class AssessmentComponentMarksDaoTest extends PersistenceTestBase {

  val dao = new AutowiringAssessmentComponentMarksDao

  @Before def setup(): Unit = {
    dao.sessionFactory = sessionFactory
  }

  private trait Fixture {
    val dept: Department = Fixtures.department("in")
    val module: Module = Fixtures.module("in101")

    dept.modules.add(module)
    module.adminDepartment = dept

    session.save(dept)
    session.save(module)

    val assessmentComponent = new AssessmentComponent
    assessmentComponent.moduleCode = "in101-10"
    assessmentComponent.assessmentGroup = "A"
    assessmentComponent.sequence = "A02"
    assessmentComponent.module = module
    assessmentComponent.assessmentType = AssessmentType.Essay
    assessmentComponent.name = "Cool Essay"
    assessmentComponent.inUse = true

    val upstreamAssessmentGroup = new UpstreamAssessmentGroup
    upstreamAssessmentGroup.moduleCode = "in101-10"
    upstreamAssessmentGroup.occurrence = "A"
    upstreamAssessmentGroup.assessmentGroup = "A"
    upstreamAssessmentGroup.sequence = "A02"
    upstreamAssessmentGroup.academicYear = AcademicYear(2010)

    val uagm1 = new UpstreamAssessmentGroupMember(upstreamAssessmentGroup, "0672089", UpstreamAssessmentGroupMemberAssessmentType.OriginalAssessment)
    val uagm2 = new UpstreamAssessmentGroupMember(upstreamAssessmentGroup, "1000005", UpstreamAssessmentGroupMemberAssessmentType.Reassessment, Some("001"))

    upstreamAssessmentGroup.members = JArrayList(
      uagm1,
      uagm2
    )

    session.save(assessmentComponent)
    session.save(upstreamAssessmentGroup)
  }

  @Test def crud(): Unit = transactional { _ =>
    new Fixture {
      dao.getRecordedStudent(uagm1) should be (empty)
      dao.getRecordedStudent(uagm2) should be (empty)

      dao.saveOrUpdate(new RecordedAssessmentComponentStudent(uagm1))
      dao.saveOrUpdate(new RecordedAssessmentComponentStudent(uagm2))

      dao.getRecordedStudent(uagm1) should be (defined)
      dao.getRecordedStudent(uagm2) should be (defined)
    }
  }

}
