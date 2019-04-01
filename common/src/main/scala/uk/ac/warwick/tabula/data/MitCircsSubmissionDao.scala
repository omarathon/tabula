package uk.ac.warwick.tabula.data

import org.hibernate.FlushMode
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
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
}

@Repository
class MitCircsSubmissionDaoImpl extends MitCircsSubmissionDao
  with Daoisms with TaskBenchmarking with AutowiringUserLookupComponent {

  def getById(id: String): Option[MitigatingCircumstancesSubmission] = getById[MitigatingCircumstancesSubmission](id)

  def getByKey(key: Long): Option[MitigatingCircumstancesSubmission] =
    session.newQuery[MitigatingCircumstancesSubmission]("from MitigatingCircumstancesSubmission where key = :key").setLong("key", key).uniqueResult

  override def saveOrUpdate(submission: MitigatingCircumstancesSubmission): MitigatingCircumstancesSubmission = {
    // fetch a new key if required
    if (submission.key == null){
      // set the flush mode to commit to avoid TransientObjectExceptions when fetching the next key
      session.setHibernateFlushMode(FlushMode.COMMIT)
      submission.key = session.createNativeQuery("select nextval('mit_circ_sequence')").getSingleResult.asInstanceOf[java.math.BigInteger].longValue
      // set the flush mode back
      session.setHibernateFlushMode(FlushMode.AUTO)
    }
    session.saveOrUpdate(submission)
    submission
  }
}