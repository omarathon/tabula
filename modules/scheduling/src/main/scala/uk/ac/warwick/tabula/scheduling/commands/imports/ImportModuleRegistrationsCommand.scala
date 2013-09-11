package uk.ac.warwick.tabula.scheduling.commands.imports

import org.joda.time.DateTime
import org.springframework.beans.BeanWrapperImpl
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.ModuleRegistrationDao
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.ModuleRegistration
import uk.ac.warwick.tabula.data.model.ModuleSelectionStatus
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.scheduling.helpers.PropertyCopying
import uk.ac.warwick.tabula.scheduling.services.ModuleRegistrationRow
import uk.ac.warwick.tabula.data.StudentCourseDetailsDao
import org.springframework.beans.BeanWrapper


class ImportModuleRegistrationsCommand(modRegRow: ModuleRegistrationRow) extends Command[Option[ModuleRegistration]]
	with Logging with Unaudited with PropertyCopying {

	PermissionCheck(Permissions.ImportSystemData)

	var moduleRegistrationDao = Wire.auto[ModuleRegistrationDao]
	var studentCourseDetailsDao = Wire.auto[StudentCourseDetailsDao]

	var scjCode = modRegRow.scjCode
	var tabulaModule = modRegRow.tabulaModule
	var academicYear = modRegRow.academicYear
	var cats: java.math.BigDecimal = modRegRow.cats
	var assessmentGroup = modRegRow.assessmentGroup
	var selectionStatusCode = modRegRow.selectionStatusCode
	var selectionStatus: ModuleSelectionStatus = null

	override def applyInternal(): Option[ModuleRegistration] = transactional() ({
		tabulaModule match {
			case None => {
				logger.warn("No stem module " + modRegRow.sitsModuleCode + " found in Tabula for " + scjCode + " - maybe it hasn't been imported yet?")
				None
			}
			case Some(module: Module) => {
				logger.debug("Importing module registration for student " + scjCode + ", module " + modRegRow.sitsModuleCode + " in " + academicYear)

				val scd = studentCourseDetailsDao.getByScjCode(scjCode).getOrElse(throw new IllegalStateException("Can't record module registration - could not find a StudentCourseDetails for " + scjCode))
				val moduleRegistrationExisting: Option[ModuleRegistration] = moduleRegistrationDao.getByNotionalKey(scd, module, cats, academicYear)

				val isTransient = !moduleRegistrationExisting.isDefined

				val moduleRegistration = moduleRegistrationExisting match {
					case Some(moduleRegistration: ModuleRegistration) => moduleRegistration
					case _ => {
						val newModuleRegistration = new ModuleRegistration(scd, module, cats, academicYear)
						module.moduleRegistrations.add(newModuleRegistration)
						newModuleRegistration
					}
				}

				val commandBean = new BeanWrapperImpl(ImportModuleRegistrationsCommand.this)
				val moduleRegistrationBean = new BeanWrapperImpl(moduleRegistration)

				val hasChanged = copyBasicProperties(properties, commandBean, moduleRegistrationBean) |
					copySelectionStatus(moduleRegistrationBean, selectionStatusCode)


				if (isTransient || hasChanged) {
					logger.debug("Saving changes for " + moduleRegistration)

					moduleRegistration.lastUpdatedDate = DateTime.now
					moduleRegistrationDao.saveOrUpdate(moduleRegistration)
				}

				Some(moduleRegistration)
			}
		}
	})

	def copySelectionStatus(destinationBean: BeanWrapper, selectionStatusCode: String) = {
		val property = "selectionStatus"
		val oldValue = destinationBean.getPropertyValue(property)
		val newValue = ModuleSelectionStatus.fromCode(selectionStatusCode)

		logger.debug("Property " + property + ": " + oldValue + " -> " + newValue)

		// null == null in Scala so this is safe for unset values
		if (oldValue != newValue) {
			logger.debug("Detected property change for " + property + " (" + oldValue + " -> " + newValue + "); setting value")

			destinationBean.setPropertyValue(property, newValue)
			true
		}
		else false
	}

	private val properties = Set(
		"assessmentGroup", "selectionStatus"
	)

	override def describe(d: Description) = d.properties(
		"scjCode" -> scjCode,
		"moduleCode" -> modRegRow.sitsModuleCode,
		"academicYear" -> academicYear
	)
}
