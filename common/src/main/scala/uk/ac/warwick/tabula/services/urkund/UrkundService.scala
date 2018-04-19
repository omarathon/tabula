package uk.ac.warwick.tabula.services.urkund

import java.io.File

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.io.Files
import org.apache.commons.io.FilenameUtils._
import org.apache.http._
import org.apache.http.auth.{Credentials, UsernamePasswordCredentials}
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.client.{HttpResponseException, ResponseHandler}
import org.apache.http.entity.ContentType
import org.apache.http.impl.client.{BasicResponseHandler, CloseableHttpClient, HttpClients}
import org.apache.http.util.EntityUtils
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.{DisposableBean, InitializingBean}
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment, OriginalityReport, Submission}
import uk.ac.warwick.tabula.data.{AutowiringUrkundDaoComponent, UrkundDaoComponent}
import uk.ac.warwick.tabula.helpers.{ApacheHttpClientUtils, Logging}
import uk.ac.warwick.util.core.StringUtils

import scala.util.parsing.json.JSON
import scala.util.{Failure, Success, Try}

object UrkundService {
	final val responseTimeout: Int = 60 * 1000
	final val reportTimeoutInMinutes = 30
	final val serverErrorTimeoutInMinutes = 15
	final val baseUrl: String = "https://secure.urkund.com/api"
	final val documentBaseUrl = s"$baseUrl/rest/submissions"
	final val receiversBaseUrl = s"$baseUrl/receivers"

	private def getBytesUtf8 = org.apache.commons.codec.binary.StringUtils.getBytesUtf8 _
	private def encodeBase64String = org.apache.commons.codec.binary.Base64.encodeBase64String _
	def urkundSafeFilename(attachment: FileAttachment): String = encodeBase64String(getBytesUtf8(StringUtils.safeSubstring(attachment.name, 0, 256)))

	def submitterAddress(submission: Submission) =
		s"${submission.studentIdentifier}.tabula@submitters.urkund.com"

	def receiverAddress(report: OriginalityReport, prefix: String): String =
		"%s.%s.tabula.%s@analysis.urkund.com".format(
			report.attachment.submissionValue.submission.assignment.id.replace("-", ""),
			report.attachment.submissionValue.submission.assignment.module.code,
			prefix
		)

	// TODO this should really be a real e-mail address, but this will do for the time being
	def receiverEmailAddress(report: OriginalityReport): String =
		"%s.%s.tabula@warwick.ac.uk".format(
			report.attachment.submissionValue.submission.assignment.id.replace("-", ""),
			report.attachment.submissionValue.submission.assignment.module.code
		)

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
	val maxFileSize: Int = maxFileSizeInMegabytes * 1024 * 1024  // 20M

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

	var objectMapper: ObjectMapper = Wire[ObjectMapper]

	@Value("${Urkund.username}") var username: String = _
	@Value("${Urkund.password}") var password: String = _
	lazy val credentials: Credentials = new UsernamePasswordCredentials(username, password)

	@Value("${Urkund.unit}") var unit: JInteger = _
	@Value("${Urkund.organization}") var organization: JInteger = _
	@Value("${Urkund.subOrganization}") var subOrganization: JInteger = _
	@Value("${Urkund.analysisPrefix}") var analysisPrefix: String = _

	private def documentUrl(report: OriginalityReport): String = "%s/%s/%s".format(
		UrkundService.documentBaseUrl,
		UrkundService.receiverAddress(report, analysisPrefix),
		report.id
	)

	private def receiverUrl(report: OriginalityReport): String = "%s/%s".format(
		UrkundService.receiversBaseUrl,
		UrkundService.receiverAddress(report, analysisPrefix)
	)

	val httpClient: CloseableHttpClient =
		HttpClients.custom()
			.setDefaultRequestConfig(RequestConfig.custom()
				.setConnectTimeout(UrkundService.responseTimeout)
				.setSocketTimeout(UrkundService.responseTimeout)
				.build())
			.disableRedirectHandling()
			.disableCookieManagement()
			.build()

	override def destroy() {
		httpClient.close()
	}

	override def afterPropertiesSet() {}

	override def findReportToSubmit: Option[OriginalityReport] =
		urkundDao.findReportToSubmit

	private def getReceiverAddress(report: OriginalityReport): Try[String] = {
		val req = new HttpGet(receiverUrl(report))
		req.setHeader(ApacheHttpClientUtils.basicAuthHeader(credentials))
		req.setHeader("Accept", "application/json")

		Try(httpClient.execute(req, new BasicResponseHandler {
			override def handleEntity(entity: HttpEntity): String = {
				EntityUtils.consumeQuietly(entity)
				UrkundService.receiverAddress(report, analysisPrefix)
			}
		})) match {
			case Success(response) => Success(response)
			case Failure(e: HttpResponseException) if e.getStatusCode == HttpStatus.SC_NOT_FOUND => createReceiverAddress(report)
			case failure => failure
		}
	}

	private def createReceiverAddress(report: OriginalityReport): Try[String] = {
		val expectedReceiverAddress = UrkundService.receiverAddress(report, analysisPrefix)
		logger.info(s"Could not find existing Urkund receiver $expectedReceiverAddress, so creating new receiver")
		val postData: String = objectMapper.writeValueAsString(Map(
			"UnitId" -> unit,
			"OrganizationId" -> organization,
			"SubOrganizationId" -> subOrganization,
			"FullName" -> "Tabula Receiver",
			"EmailAddress" -> UrkundService.receiverEmailAddress(report),
			"AnalysisAddress" -> expectedReceiverAddress
		))

		val req = new HttpPost(UrkundService.receiversBaseUrl)
		req.setHeader(ApacheHttpClientUtils.basicAuthHeader(credentials))
		req.setHeader("Accept", "application/json")
		req.setEntity(
			EntityBuilder.create()
  			.setText(postData)
  			.setContentType(ContentType.APPLICATION_JSON)
  			.build()
		)

		Try {
			httpClient.execute(req, ApacheHttpClientUtils.statusCodeFilteringHandler(HttpStatus.SC_CREATED) { entity =>
				val json = EntityUtils.toString(entity)
				JSON.parseFull(json) match {
					case Some(responseJson: Map[String, Any] @unchecked) => responseJson
					case _ => throw new IllegalArgumentException (s"Could not parse response JSON: $json")
				}
			})
		} match {
			case Success(response) => response.get("AnalysisAddress") match {
				case Some(receiverAddress) if receiverAddress.asInstanceOf[String] == expectedReceiverAddress =>
					Success(expectedReceiverAddress)
				case Some(receiverAddress) =>
					Failure(new IllegalArgumentException(s"New receiver created with unexpected address: $receiverAddress"))
				case _ =>
					Failure(new IllegalArgumentException(s"Could not find analysis address in JSON: ${objectMapper.writeValueAsString(response)}"))
			}
			case Failure(e) => Failure(e)
		}
	}

	override def submit(report: OriginalityReport): Try[UrkundResponse] = {
		getReceiverAddress(report) match {
			case Success(_) =>
				val tempFile = File.createTempFile(report.attachment.id, null)
				report.attachment.asByteSource.copyTo(Files.asByteSink(tempFile))

				val req = new HttpPost(documentUrl(report))
				req.setHeader(ApacheHttpClientUtils.basicAuthHeader(credentials))
				req.setHeader("Accept", "application/json")
				req.setHeader("x-urkund-filename", UrkundService.urkundSafeFilename(report.attachment))
				req.setHeader("x-urkund-submitter", UrkundService.submitterAddress(report.attachment.submissionValue.submission))
				req.setEntity(
					EntityBuilder.create()
  					.setFile(tempFile)
  					.setContentType(ContentType.create(UrkundService.mimeTypeConversion(report.attachment)))
  					.build()
				)

				Try {
					val handler: ResponseHandler[UrkundResponse] = ApacheHttpClientUtils.handler {
						case response if response.getStatusLine.getStatusCode == HttpStatus.SC_ACCEPTED =>
							UrkundResponse.fromSuccessJson(HttpStatus.SC_ACCEPTED, Option(response.getEntity).map(EntityUtils.toString).getOrElse(""))

						case response if response.getStatusLine.getStatusCode.toString.startsWith("4") =>
							UrkundErrorResponse(response.getStatusLine.getStatusCode, Option(response.getEntity).map(EntityUtils.toString).getOrElse(""))
					}

					httpClient.execute(req, handler)
				}
			case Failure(e) => Failure(e)
		}

	}

	override def findReportToRetreive: Option[OriginalityReport] =
		urkundDao.findReportToRetreive

	override def retrieveReport(report: OriginalityReport): Try[UrkundResponse] = {
		val req = new HttpGet(documentUrl(report))
		req.setHeader(ApacheHttpClientUtils.basicAuthHeader(credentials))
		req.setHeader("Accept", "application/json")

		Try {
			val handler: ResponseHandler[UrkundResponse] = ApacheHttpClientUtils.handler {
				case response if response.getStatusLine.getStatusCode == HttpStatus.SC_OK =>
					UrkundResponse.fromSuccessJson(HttpStatus.SC_OK, Option(response.getEntity).map(EntityUtils.toString).getOrElse(""))

				case response if response.getStatusLine.getStatusCode.toString.startsWith("4") =>
					UrkundErrorResponse(response.getStatusLine.getStatusCode, Option(response.getEntity).map(EntityUtils.toString).getOrElse(""))
			}

			httpClient.execute(req, handler)
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