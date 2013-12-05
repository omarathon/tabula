package uk.ac.warwick.tabula.web

import java.net.URLEncoder
import uk.ac.warwick.tabula.data.model.Assignment

object Routes {
	private def encoded(string: String) = URLEncoder.encode(string, "UTF-8")

	/**
	 * Warning! - These routes are not 'common'. They all assume that the current context is /coursework
	 *
	 * They need to be in common so we don't have a dependancy between coursework and common.
	 * TODO - work out a nicer way of doing this - maybe by adding a workflow URL helper to coursework
	 */

	private def assignmentroot(assignment: Assignment) =
		"/admin/module/%s/assignments/%s" format (encoded(assignment.module.code), assignment.id)

	object onlineMarkerFeedback {
		def apply(assignment: Assignment) = assignmentroot(assignment) + "/marker/feedback/online"
	}

	object onlineModeration {
		def apply(assignment: Assignment) = assignmentroot(assignment) + "/marker/feedback/online/moderation"
	}

}