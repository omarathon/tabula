package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.TopLevelUrlComponent
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.helpers.XmlUtils._

import scala.collection.JavaConverters._
import scala.xml.Elem

trait AssignmentToXmlConverter {
	self: TopLevelUrlComponent =>

	def xmlAssignmentObject(assignment: Assignment): Elem = {
		<assignment /> % Map(
			"module-code" -> assignment.module.code,
			"id" -> assignment.id,
			"open-date" -> assignment.openDate,
			"open-ended" -> assignment.openEnded,
			"close-date" -> (if (assignment.openEnded) "" else assignment.closeDate),
			"last-submission-date" -> assignment.submissions.asScala.map { _.submittedDate }.sortBy { _.getMillis }.reverse.headOption,
			"submissions-zip-url" -> (toplevelUrl + Routes.admin.assignment.submissionsZip(assignment))
		)
	}

}
