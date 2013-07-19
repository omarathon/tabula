package uk.ac.warwick.tabula.scheduling.helpers

import org.apache.log4j.Logger
import org.springframework.beans.BeanWrapper
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.services.SitsStatusesImporter
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.data.model.Course
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.SitsStatus

trait PropertyCopying extends Logging {
	var sitsStatusesImporter = Wire.auto[SitsStatusesImporter]
	var moduleAndDepartmentService = Wire.auto[ModuleAndDepartmentService]

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

	def copyObjectProperty(property: String, code: String, memberBean: BeanWrapper, obj: Object) = {
		val oldValue = memberBean.getPropertyValue(property)

		if (oldValue == null && code == null) false
		else if (oldValue == null) {
			// From no route to having a route
			memberBean.setPropertyValue(property, obj)
			true
		} else if (code == null) {
			// User had a route but now doesn't
			memberBean.setPropertyValue(property, null)
			true
		} else {
			val oldCode = oldValue match {
				case route: Route => route.code
				case course: Course => course.code
				case dept: Department => dept.code
				case sitsStatus: SitsStatus => sitsStatus.code
				case _ => null
			}
			if (oldCode == code.toLowerCase) {
				false
			} else {
				memberBean.setPropertyValue(property, obj)
				true
			}
		}
	}

	def toSitsStatus(code: String) = {
		if (code == null || code == "") {
			null
		} else {
			sitsStatusesImporter.sitsStatusMap.get(code).getOrElse(null)
		}
	}

	def toDepartment(departmentCode: String) = {
		if (departmentCode == null || departmentCode == "") {
			null
		} else {
			moduleAndDepartmentService.getDepartmentByCode(departmentCode.toLowerCase).getOrElse(null)
		}
	}
}
