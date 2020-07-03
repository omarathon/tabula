package uk.ac.warwick.tabula.services.marks

import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{RecordedResit, UpstreamAssessmentGroup}
import uk.ac.warwick.tabula.data.{AutowiringResitDaoComponent, AutowiringTransactionalComponent, ResitDaoComponent, TransactionalComponent}

trait ResitService {
  def saveOrUpdate(resit: RecordedResit): RecordedResit
  def getAllResits(uag: UpstreamAssessmentGroup): Seq[RecordedResit]
  def findResits(sprCodes: Seq[String]): Seq[RecordedResit]
  def allNeedingWritingToSits: Seq[RecordedResit]
  def mostRecentlyWrittenToSitsDate: Option[DateTime]
}

class AbstractResitService extends ResitService {
  self: ResitDaoComponent with TransactionalComponent =>

  override def saveOrUpdate(resit: RecordedResit): RecordedResit = transactional() {
    resitDao.saveOrUpdate(resit)
  }

  override def getAllResits(uag: UpstreamAssessmentGroup): Seq[RecordedResit] = transactional(readOnly = true) {
    resitDao.getAllResits(uag)
  }

  override def findResits(sprCodes: Seq[String]): Seq[RecordedResit] = transactional(readOnly = true) {
    resitDao.findResits(sprCodes)
  }

  override def allNeedingWritingToSits: Seq[RecordedResit] = transactional(readOnly = true) {
    resitDao.allNeedingWritingToSits
  }

  def mostRecentlyWrittenToSitsDate: Option[DateTime] = transactional(readOnly = true) {
    resitDao.mostRecentlyWrittenToSitsDate
  }
}


@Service("resitService")
class AutowiringResitService
  extends AbstractResitService
    with AutowiringResitDaoComponent
    with AutowiringTransactionalComponent


trait ResitServiceComponent {
  def resitService: ResitService
}

trait AutowiringResitServiceComponent extends ResitServiceComponent {
  var resitService: ResitService = Wire[ResitService]
}
