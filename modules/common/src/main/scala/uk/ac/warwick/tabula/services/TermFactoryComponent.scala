package uk.ac.warwick.tabula.services

import uk.ac.warwick.util.termdates.TermFactory
import uk.ac.warwick.spring.Wire

trait TermFactoryComponent {
	var termFactory: TermFactory
}
trait AutowiringTermFactoryComponent extends TermFactoryComponent{
	var termFactory: TermFactory = Wire[TermService]
}