package uk.ac.warwick.tabula.scheduling.commands.imports

import org.joda.time.DateTime
import org.springframework.beans.BeanWrapperImpl

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, Description, Unaudited}
import uk.ac.warwick.tabula.data.{AwardDao, Daoisms}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.Award
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.scheduling.helpers.PropertyCopying
import uk.ac.warwick.tabula.scheduling.services.AwardInfo

class ImportAwardCommand(info: AwardInfo)
	extends Command[Award] with Logging with Daoisms
	with Unaudited with PropertyCopying {

	PermissionCheck(Permissions.ImportSystemData)

	var awardDao = Wire.auto[AwardDao]

	var code = info.code
	var shortName = info.shortName
	var name = info.fullName

	override def applyInternal(): Award = transactional() {
		val awardExisting = awardDao.getByCode(code)

		logger.debug("Importing award " + code + " into " + awardExisting)

		val isTransient = !awardExisting.isDefined

		val award = awardExisting.getOrElse(new Award)

		val commandBean = new BeanWrapperImpl(this)
		val awardBean = new BeanWrapperImpl(award)

		val hasChanged = copyBasicProperties(properties, commandBean, awardBean)

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + award)

			award.lastUpdatedDate = DateTime.now
			awardDao.saveOrUpdate(award)
		}

		award
	}

	private val properties = Set(
		"code", "shortName", "name"
	)

	override def describe(d: Description) = d.property("shortName" -> shortName)

}
