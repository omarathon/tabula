package uk.ac.warwick.tabula.services.mitcircs

import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesMessage, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.data.{AutowiringMitCircsSubmissionDaoComponent, MitCircsSubmissionDaoComponent}

trait MitCircsSubmissionService {
  def getById(id: String): Option[MitigatingCircumstancesSubmission]
  def getByKey(key: Long): Option[MitigatingCircumstancesSubmission]
  def saveOrUpdate(submission: MitigatingCircumstancesSubmission): MitigatingCircumstancesSubmission
  def submissionsForStudent(studentMember: StudentMember): Seq[MitigatingCircumstancesSubmission]
  def submissionsForDepartment(department: Department): Seq[MitigatingCircumstancesSubmission]
  def create(message: MitigatingCircumstancesMessage): MitigatingCircumstancesMessage
  def messagesForSubmission(submission: MitigatingCircumstancesSubmission): Seq[MitigatingCircumstancesMessage]
}

abstract class AbstractMitCircsSubmissionService extends MitCircsSubmissionService {

  self: MitCircsSubmissionDaoComponent =>

  def getById(id: String): Option[MitigatingCircumstancesSubmission]
  def getByKey(key: Long): Option[MitigatingCircumstancesSubmission]
  def saveOrUpdate(submission: MitigatingCircumstancesSubmission): MitigatingCircumstancesSubmission
  def submissionsForStudent(studentMember: StudentMember): Seq[MitigatingCircumstancesSubmission]
  def submissionsForDepartment(department: Department): Seq[MitigatingCircumstancesSubmission]
  def create(message: MitigatingCircumstancesMessage): MitigatingCircumstancesMessage
  def messagesForSubmission(submission: MitigatingCircumstancesSubmission): Seq[MitigatingCircumstancesMessage]
}

@Service("mitCircsSubmissionService")
class MitCircsSubmissionServiceImpl extends AbstractMitCircsSubmissionService with AutowiringMitCircsSubmissionDaoComponent {
  def getById(id: String): Option[MitigatingCircumstancesSubmission] = transactional(readOnly = true) {
    mitCircsSubmissionDao.getById(id)
  }

  def getByKey(key: Long): Option[MitigatingCircumstancesSubmission] = transactional(readOnly = true) {
    mitCircsSubmissionDao.getByKey(key)
  }

  def saveOrUpdate(submission: MitigatingCircumstancesSubmission): MitigatingCircumstancesSubmission = transactional() {
    mitCircsSubmissionDao.saveOrUpdate(submission)
  }

  def submissionsForStudent(studentMember: StudentMember): Seq[MitigatingCircumstancesSubmission] = transactional(readOnly = true) {
    mitCircsSubmissionDao.submissionsForStudent(studentMember)
  }

  def submissionsForDepartment(department: Department): Seq[MitigatingCircumstancesSubmission] = transactional(readOnly = true) {
    mitCircsSubmissionDao.submissionsForDepartment(department)
  }

  def create(message: MitigatingCircumstancesMessage): MitigatingCircumstancesMessage = transactional() {
    mitCircsSubmissionDao.create(message)
  }

  def messagesForSubmission(submission: MitigatingCircumstancesSubmission): Seq[MitigatingCircumstancesMessage] = transactional(readOnly = true) {
    mitCircsSubmissionDao.messagesForSubmission(submission)
  }
}

trait MitCircsSubmissionServiceComponent {
  def mitCircsSubmissionService: MitCircsSubmissionService
}

trait AutowiringMitCircsSubmissionServiceComponent extends MitCircsSubmissionServiceComponent {
  var mitCircsSubmissionService: MitCircsSubmissionService = Wire[MitCircsSubmissionService]
}
