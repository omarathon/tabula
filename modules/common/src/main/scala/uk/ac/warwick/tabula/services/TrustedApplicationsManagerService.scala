package uk.ac.warwick.tabula.services

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.sso.client.trusted.TrustedApplicationsManager

trait TrustedApplicationsManagerComponent {
	def applicationManager: TrustedApplicationsManager
}

trait AutowiringTrustedApplicationsManagerComponent extends TrustedApplicationsManagerComponent {
	var applicationManager = Wire[TrustedApplicationsManager]
}
