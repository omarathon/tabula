package uk.ac.warwick.tabula.coursework.commands.turnitin

import uk.ac.warwick.tabula.commands.Command

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.coursework.services.turnitin._
import uk.ac.warwick.spring.Wire


trait TurnitinTrait {
	var api: Turnitin = Wire[Turnitin]
}