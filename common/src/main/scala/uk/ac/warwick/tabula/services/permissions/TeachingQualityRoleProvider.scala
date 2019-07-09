package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.roles.TeachingQualityUser

@Component
class TeachingQualityRoleProvider extends WebgroupRoleProvider(TeachingQualityUser()) {
  override var webgroup: String = Wire.property("${permissions.teachingquality.group}")
}