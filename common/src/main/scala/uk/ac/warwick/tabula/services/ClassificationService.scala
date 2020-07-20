package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Classification
import uk.ac.warwick.tabula.data.{AutowiringClassificationDaoComponent, ClassificationDaoComponent}
import uk.ac.warwick.tabula.helpers.StringUtils._

/**
 * Handles data about classifications
 */
trait ClassificationServiceComponent {
  def classificationService: ClassificationService
}


trait AutowiringClassificationServiceComponent extends ClassificationServiceComponent {
  var classificationService: ClassificationService = Wire[ClassificationService]
}

trait ClassificationService {
  def classificationFromCode(code: String): Option[Classification]
  def allClassifications: Seq[Classification]
}

abstract class AbstractClassificationService extends ClassificationService {
  self: ClassificationDaoComponent =>

  def classificationFromCode(code: String): Option[Classification] = code.maybeText.flatMap {
    someCode => classificationDao.getByCode(someCode.toLowerCase)
  }
  def allClassifications: Seq[Classification] = classificationDao.getAll
}

@Service("classificationService")
class ClassificationServiceImpl extends AbstractClassificationService with AutowiringClassificationDaoComponent
