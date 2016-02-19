package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.beans.{BeanWrapper, BeanWrapperImpl}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Command, Description, Unaudited}
import uk.ac.warwick.tabula.data.{ModuleRegistrationDao, StudentCourseDetailsDao}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.{Module, ModuleRegistration, ModuleSelectionStatus, StudentCourseDetails}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.scheduling.PropertyCopying
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.scheduling.ModuleRegistrationRow


class ImportModuleRegistrationsCommand(modRegRow: ModuleRegistrationRow) extends Command[Option[ModuleRegistration]]
	with Logging with Unaudited with PropertyCopying {

	PermissionCheck(Permissions.ImportSystemData)

	var moduleRegistrationDao = Wire[ModuleRegistrationDao]
	var studentCourseDetailsDao = Wire[StudentCourseDetailsDao]

	val scjCode = modRegRow.scjCode
	lazy val tabulaModule = moduleAndDepartmentService.getModuleBySitsCode(modRegRow.sitsModuleCode)
	val cats: JBigDecimal = modRegRow.cats
	val assessmentGroup = modRegRow.assessmentGroup
	val selectionStatusCode = modRegRow.selectionStatusCode
	val occurrence = modRegRow.occurrence
	val academicYear = AcademicYear.parse(modRegRow.academicYear)
	val selectionStatus: ModuleSelectionStatus = null
	val actualMark = modRegRow.actualMark
	val actualGrade = modRegRow.actualGrade
	val agreedMark = modRegRow.agreedMark
	val agreedGrade = modRegRow.agreedGrade

	override def applyInternal(): Option[ModuleRegistration] = transactional() ({
		tabulaModule match {
			case None =>
				logger.warn("No stem module for " + modRegRow.sitsModuleCode + " found in Tabula for " + scjCode + " - maybe it hasn't been imported yet?")
				None
			case Some(module: Module) =>
				logger.debug("Importing module registration for student " + scjCode + ", module " + modRegRow.sitsModuleCode)

				studentCourseDetailsDao.getByScjCode(scjCode) match {
					case None =>
						logger.warn("Can't record module registration - could not find a StudentCourseDetails for " + scjCode)
						None
					case Some(scd: StudentCourseDetails) =>
						val scd = studentCourseDetailsDao.getByScjCode(scjCode).getOrElse(
							throw new IllegalStateException("Can't record module registration - could not find a StudentCourseDetails for " + scjCode)
						)
						val moduleRegistrationExisting: Option[ModuleRegistration] = moduleRegistrationDao.getByNotionalKey(scd, module, cats, academicYear, occurrence)

						val isTransient = moduleRegistrationExisting.isEmpty

						val moduleRegistration = moduleRegistrationExisting match {
							case Some(moduleRegistration: ModuleRegistration) => moduleRegistration
							case _ =>
								new ModuleRegistration(scd, module, cats, academicYear, occurrence)
						}

						val commandBean = new BeanWrapperImpl(ImportModuleRegistrationsCommand.this)
						val moduleRegistrationBean = new BeanWrapperImpl(moduleRegistration)

						val hasChanged = copyBasicProperties(properties, commandBean, moduleRegistrationBean) |
							copySelectionStatus(moduleRegistrationBean, selectionStatusCode) |
							copyBigDecimal(moduleRegistrationBean, "actualMark", actualMark) |
							copyBigDecimal(moduleRegistrationBean, "agreedMark", agreedMark)

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
		"assessmentGroup", "occurrence", "actualGrade", "agreedGrade"
	)

	override def describe(d: Description) = d.properties("scjCode" -> scjCode)

}
