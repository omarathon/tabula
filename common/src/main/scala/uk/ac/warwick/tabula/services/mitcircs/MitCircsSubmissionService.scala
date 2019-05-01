package uk.ac.warwick.tabula.services.mitcircs

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesMessage, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.data.{AutowiringMitCircsSubmissionDaoComponent, MitCircsSubmissionDaoComponent, MitigatingCircumstancesSubmissionFilter, ScalaRestriction}

trait MitCircsSubmissionService {
  def getById(id: String): Option[MitigatingCircumstancesSubmission]
  def getByKey(key: Long): Option[MitigatingCircumstancesSubmission]
  def saveOrUpdate(submission: MitigatingCircumstancesSubmission): MitigatingCircumstancesSubmission
  def submissionsForStudent(studentMember: StudentMember): Seq[MitigatingCircumstancesSubmission]
  def submissionsForDepartment(department: Department, studentRestrictions: Seq[ScalaRestriction], filter: MitigatingCircumstancesSubmissionFilter): Seq[MitigatingCircumstancesSubmission]
  def create(message: MitigatingCircumstancesMessage): MitigatingCircumstancesMessage
  def messagesForSubmission(submission: MitigatingCircumstancesSubmission): Seq[MitigatingCircumstancesMessage]
}

abstract class AbstractMitCircsSubmissionService extends MitCircsSubmissionService {
  self: MitCircsSubmissionDaoComponent =>

  override def getById(id: String): Option[MitigatingCircumstancesSubmission] = transactional(readOnly = true) {
    mitCircsSubmissionDao.getById(id)
  }

  override def getByKey(key: Long): Option[MitigatingCircumstancesSubmission] = transactional(readOnly = true) {
    mitCircsSubmissionDao.getByKey(key)
  }

  override def saveOrUpdate(submission: MitigatingCircumstancesSubmission): MitigatingCircumstancesSubmission = transactional() {
    mitCircsSubmissionDao.saveOrUpdate(submission)
  }

  override def submissionsForStudent(studentMember: StudentMember): Seq[MitigatingCircumstancesSubmission] = transactional(readOnly = true) {
    mitCircsSubmissionDao.submissionsForStudent(studentMember)
  }

  override def submissionsForDepartment(department: Department, studentRestrictions: Seq[ScalaRestriction], filter: MitigatingCircumstancesSubmissionFilter): Seq[MitigatingCircumstancesSubmission] = transactional(readOnly = true) {
    mitCircsSubmissionDao.submissionsForDepartment(department, studentRestrictions, filter)
  }

  override def create(message: MitigatingCircumstancesMessage): MitigatingCircumstancesMessage = transactional() {
    mitCircsSubmissionDao.create(message)
  }

  override def messagesForSubmission(submission: MitigatingCircumstancesSubmission): Seq[MitigatingCircumstancesMessage] = transactional(readOnly = true) {
    mitCircsSubmissionDao.messagesForSubmission(submission)
  }
}

@Service("mitCircsSubmissionService")
class AutowiredMitCircsSubmissionService
  extends AbstractMitCircsSubmissionService
    with AutowiringMitCircsSubmissionDaoComponent

trait MitCircsSubmissionServiceComponent {
  def mitCircsSubmissionService: MitCircsSubmissionService
}

trait AutowiringMitCircsSubmissionServiceComponent extends MitCircsSubmissionServiceComponent {
  var mitCircsSubmissionService: MitCircsSubmissionService = Wire[MitCircsSubmissionService]
}
