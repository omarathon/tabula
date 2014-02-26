package uk.ac.warwick.tabula.scheduling.helpers

import org.springframework.beans.BeanWrapper
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.services.SitsStatusImporter
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.data.model.Course
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.SitsStatus

trait PropertyCopying extends Logging {
	var sitsStatusImporter = Wire[SitsStatusImporter]
	var moduleAndDepartmentService = Wire[ModuleAndDepartmentService]

	/* Basic properties are those that use primitive types + String + DateTime etc, so can be updated with a simple equality check and setter */
	 def copyBasicProperties(properties: Set[String], commandBean: BeanWrapper, destinationBean: BeanWrapper) = {
		// Transform the set of properties to a set of booleans saying whether the value has changed
		// It's okay to yield a Set here because we only care if it has any values that are true, no point in storing 300 trues
		val changedProperties = for (property <- properties) yield {
			val oldValue = destinationBean.getPropertyValue(property)
			val newValue = commandBean.getPropertyValue(property)

			logger.debug("Property " + property + ": " + oldValue + " -> " + newValue)

			// null == null in Scala so this is safe for unset values
			if (oldValue != newValue) {
				logger.debug("Detected property change for " + property + " (" + oldValue + " -> " + newValue + "); setting value")

				destinationBean.setPropertyValue(property, newValue)
				true
			} else false
		}

		// Fold the set of booleans left with an || of false; this uses foldLeft rather than reduceLeft to handle the empty set
		changedProperties.foldLeft(false)(_ || _)
	}

	// returns true if there is a change, false if no change
	def copyObjectProperty(property: String, code: String, memberBean: BeanWrapper, optionObj: Option[Object]) = {

		val oldValue = memberBean.getPropertyValue(property)

		optionObj match {
			case None => {
				if (oldValue == null) false // null before and still null - no change
				else {
					memberBean.setPropertyValue(property, null)  // change from non-null to null
					true
				}
			}
			case Some(obj) => {
				if (oldValue == null) { // changed from null to non-null
					memberBean.setPropertyValue(property, obj)
					true
				}
				else {
					val oldCode = oldValue match {
						case route: Route => route.code
						case course: Course => course.code
						case dept: Department => dept.code
						case sitsStatus: SitsStatus => sitsStatus.code
						case _ => null
					}
					if (oldCode == code.toLowerCase) { // same non-null value
						false
					}
					else { // different non-null value
						memberBean.setPropertyValue(property, obj)
						true
					}
				}
			}
		}
	}

	def markAsSeenInSits(bean: BeanWrapper) = {
		val propertyName = "missingFromImportSince"
		if (bean.getPropertyValue(propertyName) == null)
			false
		else {
			bean.setPropertyValue(propertyName, null)
			true
		}
	}

	def toSitsStatus(code: String): Option[SitsStatus] = {
		if (code == null || code == "") {
			None
		} else {
			sitsStatusImporter.getSitsStatusForCode(code)
		}
	}

	def toDepartment(departmentCode: String): Option[Department] = {
		if (departmentCode == null || departmentCode == "") {
			None
		} else {
			moduleAndDepartmentService.getDepartmentByCode(departmentCode)
		}
	}
}
