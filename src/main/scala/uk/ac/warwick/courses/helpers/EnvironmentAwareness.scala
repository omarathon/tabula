package uk.ac.warwick.courses.helpers

import org.springframework.context.EnvironmentAware
import org.springframework.core.env.Environment

/**
 * A Spring bean extended with this trait will have a property "environment"
 * set to the current Environment, from which you can get active profiles.
 */
trait EnvironmentAwareness extends EnvironmentAware {

	var environment: Environment = null
	def setEnvironment(env: Environment): Unit = { environment = env }

}