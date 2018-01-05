package uk.ac.warwick.tabula.web

import java.net.URLEncoder

import uk.ac.warwick.tabula.data.model.{FileAttachment, FileAttachmentToken, Submission}

object RoutesUtils {
	def encoded(string: String): String = URLEncoder.encode(string, "UTF-8")
}

object Routes {

	import uk.ac.warwick.tabula
	val cm2        = tabula.cm2.web.Routes
	@deprecated("Always use CM2 Routes", since = "209")
	val coursework = tabula.coursework.web.Routes
	val exams      = tabula.exams.web.Routes
	val profiles   = tabula.profiles.web.Routes
	val groups     = tabula.groups.web.Routes
	val attendance = tabula.attendance.web.Routes
	val admin      = tabula.admin.web.Routes
	val sysadmin   = tabula.sysadmin.web.Routes
	val reports    = tabula.reports.web.Routes
	val api        = tabula.api.web.Routes

	object turnitinlti {
		def fileByToken(submission: Submission, attachment: FileAttachment, token: FileAttachmentToken) =
			s"/turnitin/submission/${submission.id}/attachment/${attachment.id}?token=${token.id}"
	}

}