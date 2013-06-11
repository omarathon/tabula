package uk.ac.warwick.tabula.scheduling.helpers

import org.apache.log4j.Logger
import org.springframework.beans.BeanWrapper
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.model.SitsStatus
import uk.ac.warwick.tabula.scheduling.services.SitsStatusesImporter
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService

trait SitsPropertyCopying extends Logging {
		
	var sitsStatusesImporter = Wire.auto[SitsStatusesImporter]
	var moduleAndDepartmentService = Wire.auto[ModuleAndDepartmentService]

	def copyStatus(property: String, code: String, bean: BeanWrapper) = {

		val oldValue = bean.getPropertyValue(property) match {
			case null => null
			case value: SitsStatus => value
		}

		if (oldValue == null && code == null) false
		else if (oldValue == null) {
			// From no SPR status to having an SPR status
			bean.setPropertyValue(property, toSitsStatus(code))
			true
		} else if (code == null) {
			// User had an SPR status code but now doesn't
			bean.setPropertyValue(property, null)
			true
		} else if (oldValue.code == code.toLowerCase) {
			false
		}	else {
			bean.setPropertyValue(property, toSitsStatus(code))
			true
		}
	}

	private def toSitsStatus(code: String) = {
		if (code == null || code == "") {
			null
		} else {
			sitsStatusesImporter.sitsStatusMap.get(code).getOrElse(null)
		}
	}
	
	def copyDepartment(property: String, departmentCode: String, bean: BeanWrapper) = {
		val oldValue = bean.getPropertyValue(property) match {
			case null => null
			case value: Department => value
		}

		if (oldValue == null && departmentCode == null) false
		else if (oldValue == null) {
			// From no department to having a department
			bean.setPropertyValue(property, toDepartment(departmentCode))
			true
		} else if (departmentCode == null) {
			// User had a department but now doesn't
			bean.setPropertyValue(property, null)
			true
		} else if (oldValue.code == departmentCode.toLowerCase) {
			false
		}	else {
			bean.setPropertyValue(property, toDepartment(departmentCode))
			true
		}
	}

	private def toDepartment(departmentCode: String) = {
		if (departmentCode == null || departmentCode == "") {
			null
		} else {
			moduleAndDepartmentService.getDepartmentByCode(departmentCode.toLowerCase).getOrElse(null)
		}
	}	
}
