package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.roles.AcademicOfficeUser

@Component
class AcademicOfficeRoleProvider extends WebgroupRoleProvider(AcademicOfficeUser()) {
	override var webgroup: String = Wire.property("${permissions.academicoffice.group}")
}
