package uk.ac.warwick.courses.commands.turnitin

import uk.ac.warwick.courses.commands.Command
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.services.turnitin._
import uk.ac.warwick.spring.Wire


trait TurnitinTrait {
	var api: Turnitin = Wire.auto[Turnitin]
}