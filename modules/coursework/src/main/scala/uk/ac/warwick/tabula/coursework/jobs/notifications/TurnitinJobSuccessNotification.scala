package uk.ac.warwick.tabula.coursework.jobs.notifications

import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.tabula.coursework.services.turnitin.TurnitinSubmissionInfo
import java.util.HashMap

class TurnitinJobSuccessNotification(failedUploads: HashMap[String, String], reports:Seq[TurnitinSubmissionInfo] ,assignment: Assignment, user: User)
	extends TurnitinReportNotification(reports, assignment, user) {

	this: TextRenderer =>

	def title = "Turnitin check finished for %s - %s" format (assignment.module.code.toUpperCase, assignment.name)

	def content = renderTemplate("/WEB-INF/freemarker/emails/turnitinjobdone.ftl", Map(
		"assignment" -> assignment,
		"assignmentTitle" -> ("%s - %s" format (assignment.module.code.toUpperCase, assignment.name)),
		"user" -> user,
		"failureCount" -> failedUploads.size(),
		"failedUploads" -> failedUploads,
		"path" -> url
	))
}