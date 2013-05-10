package uk.ac.warwick.tabula.system

import scala.annotation.meta.getter

package object permissions {
	
	/** Add this annotation to a property to restrict its use in views.
	 *  Don't use RestrictedInternal directly because by default an annotation will
	 *  only get added to the private field, not the getter or setter. */
	type Restricted = RestrictedInternal @getter
	
}