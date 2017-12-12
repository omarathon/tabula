package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.beans.{BeanWrapper, PropertyAccessorFactory}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Command, Description, Unaudited}
import uk.ac.warwick.tabula.data.ModuleRegistrationDao
import uk.ac.warwick.tabula.data.model.{Module, ModuleRegistration, ModuleSelectionStatus, StudentCourseDetails}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.scheduling.PropertyCopying
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.scheduling.ModuleRegistrationRow

class ImportModuleRegistrationsCommand(course: StudentCourseDetails, courseRows: Seq[ModuleRegistrationRow], modules: Set[Module])
	extends Command[Seq[ModuleRegistration]] with Logging with Unaudited with PropertyCopying {

	PermissionCheck(Permissions.ImportSystemData)

	var moduleRegistrationDao: ModuleRegistrationDao = Wire[ModuleRegistrationDao]


	override def applyInternal(): Seq[ModuleRegistration] = {
		logger.debug("Importing module registration for student " + course.scjCode)

		val records = courseRows.map { modRegRow =>
			val module = modules.find(_.code == Module.stripCats(modRegRow.sitsModuleCode).get.toLowerCase).get
			val existingRegistration = course.moduleRegistrations.find(mr =>
				mr.module.code == module.code
					&& mr.academicYear == AcademicYear.parse(modRegRow.academicYear)
					&& mr.cats == modRegRow.cats
					&& mr.occurrence == modRegRow.occurrence
			)

			val isTransient = existingRegistration.isEmpty

			val moduleRegistration = existingRegistration match {
				case Some(moduleRegistration: ModuleRegistration) =>
					moduleRegistration
				case _ =>
					val mr = new ModuleRegistration(
						course,
						module,
						modRegRow.cats,
						AcademicYear.parse(modRegRow.academicYear),
						modRegRow.occurrence
					)
					course.addModuleRegistration(mr)
					mr
			}

			val rowBean = PropertyAccessorFactory.forBeanPropertyAccess(modRegRow)
			val moduleRegistrationBean = PropertyAccessorFactory.forBeanPropertyAccess(moduleRegistration)

			val hasChanged = copyBasicProperties(properties, rowBean, moduleRegistrationBean) |
				copySelectionStatus(moduleRegistrationBean, modRegRow.selectionStatusCode) |
				copyBigDecimal(moduleRegistrationBean, "actualMark", modRegRow.actualMark) |
				copyBigDecimal(moduleRegistrationBean, "agreedMark", modRegRow.agreedMark)

			if (isTransient || hasChanged || moduleRegistration.deleted) {
				logger.debug("Saving changes for " + moduleRegistration)

				moduleRegistration.deleted = false
				moduleRegistration.lastUpdatedDate = DateTime.now
				moduleRegistrationDao.saveOrUpdate(moduleRegistration)
			}

			moduleRegistration

		}
		markDeleted(course, courseRows)
		records
	}


	def markDeleted(studentCourse: StudentCourseDetails, courseRows: Seq[ModuleRegistrationRow]): Unit = {
		studentCourse.moduleRegistrations.filterNot(_.deleted).foreach { mr =>
			val mrExists = courseRows.exists { sitsMR =>
				(modules.find(_.code == Module.stripCats(sitsMR.sitsModuleCode).get.toLowerCase)
					.exists { module => module.code == mr.module.code }
					&& AcademicYear.parse(sitsMR.academicYear) == mr.academicYear
					&& sitsMR.cats == mr.cats
					&& sitsMR.occurrence == mr.occurrence)
			}

			/** Ensure at least there is some record in SITS.If for some reason we couldn't
				* get any SITS data, don't mark all deleted- better to leave as it is **/
			if (courseRows.nonEmpty && !mrExists) {
				mr.markDeleted
				logger.info("Marking delete  for " + mr)
				mr.lastUpdatedDate = DateTime.now
				moduleRegistrationDao.saveOrUpdate(mr)
			}
		}
	}

	def copySelectionStatus(destinationBean: BeanWrapper, selectionStatusCode: String): Boolean = {
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

	override def describe(d: Description): Unit = d.properties("scjCode" -> course.scjCode)

}
