package uk.ac.warwick.tabula.scheduling.helpers

import org.apache.log4j.Logger
import org.springframework.beans.BeanWrapper

trait PropertyCopying {
	/* Basic properties are those that use primitive types + String + DateTime etc, so can be updated with a simple equality check and setter */
	 def copyBasicProperties(properties: Set[String], commandBean: BeanWrapper, destinationBean: BeanWrapper, logger: Logger) = {
		// Transform the set of properties to a set of booleans saying whether the value has changed
		val changedProperties = for (property <- properties) yield {
			val oldValue = destinationBean.getPropertyValue(property)
			val newValue = commandBean.getPropertyValue(property)
			
			logger.debug("Property " + property + ": " + oldValue + " -> " + newValue)
			
			// null == null in Scala so this is safe for unset values
			if (oldValue != newValue) {
				logger.debug("Detected property change; setting value")
				
				destinationBean.setPropertyValue(property, newValue)
				true
			} else false
		}
		
		// Fold the set of booleans left with an || of false; this uses foldLeft rather than reduceLeft to handle the empty set
		changedProperties.foldLeft(false)(_ || _)
	}
}
