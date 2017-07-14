package uk.ac.warwick.tabula.commands.cm2.turnitin

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.turnitin.Turnitin


trait HasTurnitinApi {
	var api: Turnitin = Wire[Turnitin]
}