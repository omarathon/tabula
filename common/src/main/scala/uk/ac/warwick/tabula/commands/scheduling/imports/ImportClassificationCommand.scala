package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.beans.BeanWrapperImpl
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, Description, Unaudited}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.Classification
import uk.ac.warwick.tabula.data.{ClassificationDao, Daoisms}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.scheduling.PropertyCopying
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.scheduling.ClassificationInfo

class ImportClassificationCommand(info: ClassificationInfo)
  extends Command[(Classification, ImportAcademicInformationCommand.ImportResult)] with Logging with Daoisms
    with Unaudited with PropertyCopying {

  PermissionCheck(Permissions.ImportSystemData)

  var classificationDao: ClassificationDao = Wire.auto[ClassificationDao]

  var code: String = info.code
  var shortName: String = info.shortName
  var name: String = info.fullName

  override def applyInternal(): (Classification, ImportAcademicInformationCommand.ImportResult) = transactional() {
    val classificationExisting = classificationDao.getByCode(code)

    logger.debug("Importing classification " + code + " into " + classificationExisting)

    val isTransient = classificationExisting.isEmpty

    val classification = classificationExisting.getOrElse(new Classification)

    val commandBean = new BeanWrapperImpl(this)
    val classificationBean = new BeanWrapperImpl(classification)

    val hasChanged = copyBasicProperties(properties, commandBean, classificationBean)

    if (isTransient || hasChanged) {
      logger.debug("Saving changes for " + classification)

      classification.lastUpdatedDate = DateTime.now
      classificationDao.saveOrUpdate(classification)
    }

    val result =
      if (isTransient) ImportAcademicInformationCommand.ImportResult(added = 1)
      else if (hasChanged) ImportAcademicInformationCommand.ImportResult(deleted = 1)
      else ImportAcademicInformationCommand.ImportResult()

    (classification, result)
  }

  private val properties = Set(
    "code", "shortName", "name"
  )

  override def describe(d: Description): Unit = d.property("shortName" -> shortName)

}
