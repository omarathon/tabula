package uk.ac.warwick.tabula.web.controllers.coursework.admin

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.XmlUtils._

import scala.collection.JavaConverters._
import scala.xml._

object AdminHomeExports {
	class XMLBuilder(val department: Department, val info: DepartmentHomeInformation) extends AdminHomeExport {
		var topLevelUrl: String = Wire.property("${toplevel.url}")

		def toXML: Elem = {
			val assignments = info.modules.flatMap { _.assignments.asScala }.filter { _.isAlive }

			<assignments>
				{ assignments map assignmentElement }
			</assignments>
		}

		def assignmentElement(assignment: Assignment): Elem = {
			<assignment>

			</assignment> % assignmentData(assignment)
		}
	}

	trait AdminHomeExport {
		val department: Department
		val info: DepartmentHomeInformation
		def topLevelUrl: String

		protected def assignmentData(assignment: Assignment): Map[String, Any] = Map(
			"module-code" -> assignment.module.code,
			"id" -> assignment.id,
			"open-date" -> assignment.openDate,
			"open-ended" -> assignment.openEnded,
			"close-date" -> (if (assignment.openEnded) "" else assignment.closeDate),
			"last-submission-date" -> assignment.submissions.asScala.map { _.submittedDate }.sortBy { _.getMillis }.reverse.headOption,
			"submissions-zip-url" -> (topLevelUrl + "/coursework" + Routes.admin.assignment.submissionsZip(assignment))
		)
	}
}
