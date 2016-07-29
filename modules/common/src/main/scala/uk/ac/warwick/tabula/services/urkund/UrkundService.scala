package uk.ac.warwick.tabula.services.urkund

import java.io.{File, FileOutputStream}

import dispatch.classic.thread.ThreadSafeHttpClient
import dispatch.classic.{Http, thread, _}
import org.apache.commons.io.FilenameUtils._
import org.apache.http.client.params.{ClientPNames, CookiePolicy}
import org.apache.http.impl.client.DefaultRedirectStrategy
import org.apache.http.params.HttpConnectionParams
import org.apache.http.protocol.HttpContext
import org.apache.http.{HttpRequest, HttpResponse}
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.{DisposableBean, InitializingBean}
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment, OriginalityReport, Submission}
import uk.ac.warwick.tabula.data.{AutowiringUrkundDaoComponent, UrkundDaoComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.util.core.StringUtils

import scala.util.{Failure, Success, Try}

object UrkundService {
	final val responseTimeout = 60 * 1000
	final val reportTimeoutInMinutes = 30
	final val serverErrorTimeoutInMinutes = 15

	private def getBytesUtf8 = org.apache.commons.codec.binary.StringUtils.getBytesUtf8 _
	private def encodeBase64String = org.apache.commons.codec.binary.Base64.encodeBase64String _
	def urkundSafeFilename(attachment: FileAttachment) = encodeBase64String(getBytesUtf8(StringUtils.safeSubstring(attachment.name, 0, 256)))

	def submitterAddress(submission: Submission) = s"${submission.universityId}.tabula@submitters.urkund.com"

	def setNextSubmitAttempt(report: OriginalityReport): Unit = {
		report.nextSubmitAttempt = DateTime.now.plusMinutes(
			(Math.pow(2, report.submitAttempts - 1) * serverErrorTimeoutInMinutes).toInt
		)
		if (report.submitAttempts > 7) {
			report.nextSubmitAttempt = null
		}
	}

	def setNextResponseAttempt(report: OriginalityReport): Unit = {
		if (report.responseAttempts < 5) {
			report.nextResponseAttempt = DateTime.now.plusMinutes(
				(Math.pow(2, report.responseAttempts - 1) * reportTimeoutInMinutes).toInt
			)
		} else if (report.responseAttempts <= 9) {
			report.nextResponseAttempt = DateTime.now.plusMinutes(
				(Math.pow(2, 4) * reportTimeoutInMinutes).toInt
			)
		} else {
			report.nextResponseAttempt = null
		}
	}

	def setNextResponseAttemptOnError(report: OriginalityReport): Unit = {
		report.nextResponseAttempt = DateTime.now.plusMinutes(
			(Math.pow(2, report.responseAttempts - 1) * serverErrorTimeoutInMinutes).toInt
		)
		if (report.responseAttempts > 7) {
			report.nextResponseAttempt = null
		}
	}

	val validExtensions = Seq("doc", "docx", "sxw", "ppt", "pptx", "pdf", "txt", "rtf", "html", "htm", "wps", "odt")
	val maxFileSizeInMegabytes = 20
	val maxFileSize = maxFileSizeInMegabytes * 1024 * 1024  // 20M

	def validFileType(file: FileAttachment): Boolean =
		validExtensions.contains(getExtension(file.name).toLowerCase)

	def validFileSize(file: FileAttachment): Boolean =
		file.actualDataLength < maxFileSize

	def mimeTypeConversion(attachment: FileAttachment): String = attachment.fileExt match {
		case "doc" => "application/msword"
		case "docx" => "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
		case "sxw" => "application/vnd.sun.xml.writer"
		case "ppt" => "application/vnd.ms-powerpoint"
		case "pptx" => "application/vnd.openxmlformats-officedocument.presentationml.presentation"
		case "pdf" => "application/pdf"
		case "txt" => "text/plain"
		case "rtf" => "application/rtf"
		case "html" => "text/html"
		case "htm" => "text/html"
		case "wps" => "application/vnd.ms-works"
		case "odt" => "application/vnd.oasis.opendocument.text"
		case _ => "application/octet-string"
	}
}

trait UrkundService {

	def findReportToSubmit: Option[OriginalityReport]
	def submit(report: OriginalityReport): Try[UrkundResponse]
	def findReportToRetreive: Option[OriginalityReport]
	def retrieveReport(report: OriginalityReport): Try[UrkundResponse]
	def listOriginalityReports(assignment: Assignment): Seq[OriginalityReport]

}

abstract class AbstractUrkundService extends UrkundService
	with Logging with DisposableBean with InitializingBean {

	self: UrkundDaoComponent =>

	@Value("${Urkund.username}") var username: String = _
	@Value("${Urkund.password}") var password: String = _
	@Value("${Urkund.receiver}") var receiver: String = _
	@Value("${Urkund.documentBaseUrl}") var documentBaseUrl: String = _

	private def documentUrl(report: OriginalityReport): String = s"$documentBaseUrl/$receiver/${report.id}"

	val http: Http = new Http with thread.Safety {
		override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), maxConnections, maxConnectionsPerRoute) {
			HttpConnectionParams.setConnectionTimeout(getParams, UrkundService.responseTimeout)
			HttpConnectionParams.setSoTimeout(getParams, UrkundService.responseTimeout)
			setRedirectStrategy(new DefaultRedirectStrategy {
				override def isRedirected(req: HttpRequest, res: HttpResponse, ctx: HttpContext) = false
			})
			getParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
		}
	}

	override def destroy() {
		http.shutdown()
	}

	override def afterPropertiesSet() {}

	override def findReportToSubmit: Option[OriginalityReport] =
		urkundDao.findReportToSubmit

	override def submit(report: OriginalityReport): Try[UrkundResponse] = {
		val tempFile = File.createTempFile(report.attachment.name, null)
		FileCopyUtils.copy(report.attachment.dataStream, new FileOutputStream(tempFile))

		val req = url(documentUrl(report))
			.as_!(username, password) // Add mandatory Basic auth
			.<:<(Map( // Add request headers
				"Content-Type" -> UrkundService.mimeTypeConversion(report.attachment),
				"Accept" -> "application/json",
				"x-urkund-filename" -> UrkundService.urkundSafeFilename(report.attachment),
				"x-urkund-submitter" -> UrkundService.submitterAddress(report.attachment.submissionValue.submission)
			))
			.<<<(tempFile, UrkundService.mimeTypeConversion(report.attachment)) // Attach the file (turns it into a PUT because of course it does
			.copy(method="POST") // Change it back into a POST

		Try(http.when(_==202){ req >- { json => UrkundResponse.fromSuccessJson(202, json) }}) match {
			case Success(response) => Success(response)
			case Failure(StatusCode(code, contents)) if code.toString.startsWith("4") => {
				logger.error(contents)
				Success(UrkundErrorResponse(code))
			}
			case failure => failure
		}
	}

	override def findReportToRetreive: Option[OriginalityReport] =
		urkundDao.findReportToRetreive

	override def retrieveReport(report: OriginalityReport): Try[UrkundResponse] = {
		val req = url(documentUrl(report))
			.as_!(username, password) // Add mandatory Basic auth
			.<:<(Map( // Add request headers
				"Accept" -> "application/json"
		))

		Try(http.when(_==200){ req >- { json => UrkundResponse.fromSuccessJson(200, json) }}) match {
			case Success(response) => Success(response)
			case Failure(StatusCode(code, contents)) if code.toString.startsWith("4") => Success(UrkundErrorResponse(code))
			case failure => failure
		}
	}

	override def listOriginalityReports(assignment: Assignment): Seq[OriginalityReport] =
		urkundDao.listOriginalityReports(assignment)

}

@Service("urkundService")
class UrkundServiceImpl
	extends AbstractUrkundService
	with AutowiringUrkundDaoComponent

trait UrkundServiceComponent {
	def urkundService: UrkundService
}

trait AutowiringUrkundServiceComponent extends UrkundServiceComponent {
	val urkundService: UrkundService = Wire[UrkundService]
}