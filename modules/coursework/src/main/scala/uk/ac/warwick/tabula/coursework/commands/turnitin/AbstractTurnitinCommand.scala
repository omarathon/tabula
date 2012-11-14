package uk.ac.warwick.tabula.coursework.commands.turnitin

import uk.ac.warwick.tabula.coursework.commands.Command
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.coursework.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.coursework.services.turnitin._
import uk.ac.warwick.spring.Wire


trait TurnitinTrait {
	var api: Turnitin = Wire.auto[Turnitin]
}