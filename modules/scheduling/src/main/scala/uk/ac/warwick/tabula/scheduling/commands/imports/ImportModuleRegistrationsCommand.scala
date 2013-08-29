package uk.ac.warwick.tabula.scheduling.commands.imports

import org.joda.time.DateTime
import org.springframework.beans.BeanWrapperImpl
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.SitsStatusDao
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.SitsStatus
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.scheduling.helpers.PropertyCopying
import uk.ac.warwick.tabula.scheduling.services.SitsStatusInfo
import uk.ac.warwick.tabula.data.model.ModuleRegistration
import uk.ac.warwick.tabula.data.ModuleRegistrationDao
import uk.ac.warwick.tabula.scheduling.services.ModuleRegistrationRow
import uk.ac.warwick.tabula.AcademicYear

class ImportModuleRegistrationsCommand(modRegRow: ModuleRegistrationRow) extends Command[ModuleRegistration] with Logging
	with Unaudited with PropertyCopying {

	PermissionCheck(Permissions.ImportSystemData)

	var moduleRegistrationDao = Wire.auto[ModuleRegistrationDao]

	var sprCode = modRegRow.sprCode
	var moduleCode = modRegRow.tabulaModuleCode
	var academicYear = modRegRow.academicYear
	var cats: Double = modRegRow.cats
	var assessmentGroup = modRegRow.assessmentGroup
	var selectionStatusCode = modRegRow.selectionStatusCode

	override def applyInternal(): ModuleRegistration = transactional() ({
		val moduleRegistrationExisting: Option[ModuleRegistration] = moduleRegistrationDao.getByNotionalKey(sprCode, moduleCode, cats, academicYear)

		logger.debug("Importing module registration for student " + sprCode + ", module " + moduleCode + " in " + academicYear)

		val isTransient = !moduleRegistrationExisting.isDefined

		val moduleRegistration = moduleRegistrationExisting match {
			case Some(moduleRegistration: ModuleRegistration) => moduleRegistration
			case _ => new ModuleRegistration(sprCode, moduleCode, academicYear, cats)
		}

		val commandBean = new BeanWrapperImpl(ImportModuleRegistrationsCommand.this)
		val moduleRegistrationBean = new BeanWrapperImpl(moduleRegistration)

		val hasChanged = copyBasicProperties(properties, commandBean, moduleRegistrationBean)

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + moduleRegistration)

			moduleRegistration.lastUpdatedDate = DateTime.now
			moduleRegistrationDao.saveOrUpdate(moduleRegistration)
		}

		moduleRegistration
	})

	private val properties = Set(
		"assessmentGroup", "selectionStatusCode"
	)

	override def describe(d: Description) = d.properties(
		"sprCode" -> sprCode,
		"moduleCode" -> moduleCode,
		"academicYear" -> academicYear
	)
}
