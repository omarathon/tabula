package uk.ac.warwick.tabula.services.mitcircs

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.data.{AutowiringMitCircsSubmissionDaoComponent, MitCircsSubmissionDaoComponent}

trait MitCircsSubmissionService {
  def getById(id: String): Option[MitigatingCircumstancesSubmission]
  def getByKey(key: Long): Option[MitigatingCircumstancesSubmission]
  def saveOrUpdate(submission: MitigatingCircumstancesSubmission): MitigatingCircumstancesSubmission
  def submissionsForStudent(studentMember: StudentMember): Seq[MitigatingCircumstancesSubmission]
  def submissionsForDepartment(department: Department): Seq[MitigatingCircumstancesSubmission]
}

abstract class AbstractMitCircsSubmissionService extends MitCircsSubmissionService {

  self: MitCircsSubmissionDaoComponent =>

  def getById(id: String): Option[MitigatingCircumstancesSubmission]
  def getByKey(key: Long): Option[MitigatingCircumstancesSubmission]
  def saveOrUpdate(submission: MitigatingCircumstancesSubmission): MitigatingCircumstancesSubmission
  def submissionsForStudent(studentMember: StudentMember): Seq[MitigatingCircumstancesSubmission]
  def submissionsForDepartment(department: Department): Seq[MitigatingCircumstancesSubmission]
}

@Service("mitCircsSubmissionService")
class MitCircsSubmissionServiceImpl extends AbstractMitCircsSubmissionService with AutowiringMitCircsSubmissionDaoComponent {
  def getById(id: String): Option[MitigatingCircumstancesSubmission] = mitCircsSubmissionDao.getById(id)
  def getByKey(key: Long): Option[MitigatingCircumstancesSubmission] = mitCircsSubmissionDao.getByKey(key)
  def saveOrUpdate(submission: MitigatingCircumstancesSubmission): MitigatingCircumstancesSubmission = mitCircsSubmissionDao.saveOrUpdate(submission)
  def submissionsForStudent(studentMember: StudentMember): Seq[MitigatingCircumstancesSubmission] = mitCircsSubmissionDao.submissionsForStudent(studentMember)
  def submissionsForDepartment(department: Department): Seq[MitigatingCircumstancesSubmission] = mitCircsSubmissionDao.submissionsForDepartment(department)
}

trait MitCircsSubmissionServiceComponent {
  def mitCircsSubmissionService: MitCircsSubmissionService
}

trait AutowiringMitCircsSubmissionServiceComponent extends MitCircsSubmissionServiceComponent {
  var mitCircsSubmissionService: MitCircsSubmissionService = Wire[MitCircsSubmissionService]
}
