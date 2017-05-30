package uk.ac.warwick.tabula

import uk.ac.warwick.spring.Wire

trait TopLevelUrlComponent {
	def toplevelUrl: String
}

trait AutowiringTopLevelUrlComponent extends TopLevelUrlComponent {
	val toplevelUrl: String = Wire.property("${toplevel.url}")
}
