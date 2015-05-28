package uk.ac.warwick.tabula.coursework.web.turnitinlti

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.turnitinlti.TurnitinLti


trait TurnitinLtiTrait {
	var api: TurnitinLti = Wire.auto[TurnitinLti]
}