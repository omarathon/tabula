package uk.ac.warwick.tabula.cm2.web

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.web.RoutesUtils

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 *
 * For methods called "apply", you can leave out the "apply" and treat the object like a function.
 */
object Routes {
	import RoutesUtils._

	// FIXME this isn't really an optional property, but testing is a pain unless it's made so
	var _cm2Prefix: Option[String] = Wire.optionProperty("${cm2.prefix}")
	def cm2Prefix: String = _cm2Prefix.orNull

	private lazy val context = s"/$cm2Prefix"
	def home: String = context + "/"

	object assignment {
		def apply(assignment: Assignment): String = context + s"/submission/${encoded(assignment.id)}/"
	}

	object admin {
		def apply() = s"$context/admin"

		object extensions {
			def apply(): String = admin() + "/extensions"
			def detail(extension: Extension): String = extensions() + s"/${extension.id}/detail/"
			def modify(extension: Extension): String = extensions() + s"/${extension.id}/update/"
		}

		object assignment {
			def createAssignmentDetails(module: Module): String = admin() + s"/${encoded(module.code)}/assignments/new"
			def createAddFeedback(assignment: Assignment): String = admin()  + s"/assignments/new/${encoded(assignment.id)}/feedback"
			def createAddStudents(assignment: Assignment): String = admin()  + s"/assignments/new/${encoded(assignment.id)}/students"
			def createAddMarkers(assignment: Assignment): String = admin()  + s"/assignments/new/${encoded(assignment.id)}/markers"
			def createAddSubmissions(assignment: Assignment): String = admin()  + s"/assignments/new/${encoded(assignment.id)}/submissions"
			def createAddOptions(assignment: Assignment): String = admin()  + s"/assignments/new/${encoded(assignment.id)}/options"
			def reviewAssignment(assignment: Assignment): String = admin()  + s"/assignments/new/${encoded(assignment.id)}/review"

			object audit {
				def apply(assignment: Assignment): String = admin() + s"/audit/assignment/${encoded(assignment.id)}"
			}
			def extensions(assignment: Assignment): String = admin() + s"/assignments/${encoded(assignment.id)}/manage/extensions"
		}
	}
}
