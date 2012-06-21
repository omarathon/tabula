package uk.ac.warwick.courses.commands.turnitin

import uk.ac.warwick.courses.commands.Command
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.services.turnitin._

@Configurable
abstract class AbstractTurnitinCommand[T] extends Command[T] with TurnitinTrait with Logging {
	
}

trait TurnitinTrait {
	@Autowired var api:Turnitin = _
	
	/** The name of the Turnitin Class we should store this Assignment in. */
	def classNameFor(assignment: Assignment) = "CourseworkSubmissionAPIClass"
		
	/**
	 * Create or get an assignment by this name. If the required class doesn't exist
	 * it tries to create that, and if the assignment seems to already exist it runs
	 * an update in order to get the ID back. Returns an Option of the assignment ID
	 * (this ID is generated by Turnitin).
	 */
	def createOrGetAssignment(assignment: Assignment, createClass:Boolean=true) : Option[String] = {
		val className = classNameFor(assignment)
		api.createAssignment(className, assignment.id) match {
			case Created(id) => Some(id)
			case ClassNotFound() => 
				if (createClass) {
					api.createClass(className)
					createOrGetAssignment(assignment, false)
				} else {
					// tried making a class but it didn't help. give up
					None
				}
			case AlreadyExists() =>  // do an update which will return the ID
				api.createAssignment(className, assignment.id, true) match {
					case Created(id) => Some(id)
					case _ => None
				}
			case _ => None
		}
	}
}