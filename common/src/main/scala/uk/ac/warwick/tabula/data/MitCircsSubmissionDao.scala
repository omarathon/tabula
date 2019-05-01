package uk.ac.warwick.tabula.data

import org.hibernate.FlushMode
import org.hibernate.criterion.{DetachedCriteria, Order, Projections, Property}
import org.hibernate.criterion.Restrictions._
import org.joda.time.LocalDate
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmissionState}
import uk.ac.warwick.tabula.data.model.{Department, Module, StudentMember}
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent


trait MitCircsSubmissionDaoComponent {
  val mitCircsSubmissionDao: MitCircsSubmissionDao
}

trait AutowiringMitCircsSubmissionDaoComponent extends MitCircsSubmissionDaoComponent {
  val mitCircsSubmissionDao: MitCircsSubmissionDao = Wire[MitCircsSubmissionDao]
}

trait MitCircsSubmissionDao {
  def getById(id: String): Option[MitigatingCircumstancesSubmission]
  def getByKey(key: Long): Option[MitigatingCircumstancesSubmission]
  def saveOrUpdate(submission: MitigatingCircumstancesSubmission): MitigatingCircumstancesSubmission
  def submissionsForStudent(studentMember: StudentMember): Seq[MitigatingCircumstancesSubmission]
  def submissionsForDepartment(department: Department, studentRestrictions: Seq[ScalaRestriction], filter: MitigatingCircumstancesSubmissionFilter): Seq[MitigatingCircumstancesSubmission]
}

@Repository
class MitCircsSubmissionDaoImpl extends MitCircsSubmissionDao
  with Daoisms with TaskBenchmarking with AutowiringUserLookupComponent {

  def getById(id: String): Option[MitigatingCircumstancesSubmission] = getById[MitigatingCircumstancesSubmission](id)

  def getByKey(key: Long): Option[MitigatingCircumstancesSubmission] =
    session.newQuery[MitigatingCircumstancesSubmission]("from MitigatingCircumstancesSubmission where key = :key").setLong("key", key).uniqueResult

  override def saveOrUpdate(submission: MitigatingCircumstancesSubmission): MitigatingCircumstancesSubmission = {
    // fetch a new key if required
    if (submission.key == null) {
      // set the flush mode to commit to avoid TransientObjectExceptions when fetching the next key
      session.setHibernateFlushMode(FlushMode.COMMIT)
      submission.key = session.createNativeQuery("select nextval('mit_circ_sequence')").getSingleResult.asInstanceOf[java.math.BigInteger].longValue
      // set the flush mode back
      session.setHibernateFlushMode(FlushMode.AUTO)
    }
    session.saveOrUpdate(submission)
    submission
  }

  def submissionsForStudent(studentMember: StudentMember): Seq[MitigatingCircumstancesSubmission] =
    session.newCriteria[MitigatingCircumstancesSubmission]
      .add(is("student", studentMember))
      .addOrder(Order.desc("lastModified"))
      .seq

  def submissionsForDepartment(department: Department, studentRestrictions: Seq[ScalaRestriction], filter: MitigatingCircumstancesSubmissionFilter): Seq[MitigatingCircumstancesSubmission] = {
    val c =
      session.newCriteria[MitigatingCircumstancesSubmission]
        .add(is("department", department))
        .addOrder(Order.desc("lastModified"))

    if (studentRestrictions.nonEmpty) {
      val students =
        DetachedCriteria.forClass(classOf[StudentMember])
          .setProjection(Projections.property("universityId"))

      ScalaRestriction.applyToDetached(studentRestrictions, students)

      c.add(Property.forName("student.universityId").in(students))
    }

    if (filter.affectedAssessmentModules.nonEmpty) {
      c.createAlias("affectedAssessments", "affectedAssessment")
        .add(safeIn("affectedAssessment.module", filter.affectedAssessmentModules.toSeq))
    }

    filter.includesStartDate.foreach { d =>
      c.add(
        or(
          isNull("endDate"),     // Is ongoing, or
          not(lt("endDate", d))  // Doesn't end before this date
        )
      )
    }

    filter.includesEndDate.foreach { d =>
      // Doesn't start after this date, doesn't end before this date
      c.add(
        and(
          not(gt("startDate", d)),
          isNotNull("endDate"),
          not(lt("endDate", d))
        )
      )
    }

    filter.approvedStartDate.foreach { d =>
      // The approved date must not be before this date
      c.add(
        and(
          isNotNull("_approvedOn"),
          not(lt("_approvedOn", d.toDateTimeAtStartOfDay))
        )
      )
    }

    filter.approvedEndDate.foreach { d =>
      // The approved date must not be after this date
      c.add(
        and(
          isNotNull("_approvedOn"),
          not(gt("_approvedOn", d.plusDays(1).toDateTimeAtStartOfDay.minusMillis(1)))
        )
      )
    }

    if (filter.state.nonEmpty) {
      c.add(safeIn("_state", filter.state.toSeq))
    }

    c.distinct.seq
  }
}

case class MitigatingCircumstancesSubmissionFilter(
  affectedAssessmentModules: Set[Module] = Set.empty,
  includesStartDate: Option[LocalDate] = None,
  includesEndDate: Option[LocalDate] = None,
  approvedStartDate: Option[LocalDate] = None,
  approvedEndDate: Option[LocalDate] = None,
  state: Set[MitigatingCircumstancesSubmissionState] = Set.empty,
)
