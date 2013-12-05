package uk.ac.warwick.tabula.web

import java.net.URLEncoder
import uk.ac.warwick.tabula.data.model.Assignment

object Routes {
	private def encoded(string: String) = URLEncoder.encode(string, "UTF-8")

	private def assignmentroot(assignment: Assignment) =
		"/admin/module/%s/assignments/%s" format (encoded(assignment.module.code), assignment.id)

	object onlineMarkerFeedback {
		def apply(assignment: Assignment) = assignmentroot(assignment) + "/marker/feedback/online"
	}

	object onlineModeration {
		def apply(assignment: Assignment) = assignmentroot(assignment) + "/marker/feedback/online/moderation"
	}
}