package uk.ac.warwick.tabula.services.turnitinlti

import java.io.IOException

import dispatch.classic.Request.toRequestVerbs
import dispatch.classic._
import dispatch.classic.thread.ThreadSafeHttpClient
import org.apache.http.{HttpStatus, HttpRequest, HttpResponse}
import org.apache.http.client.params.{ClientPNames, CookiePolicy}
import org.apache.http.impl.client.DefaultRedirectStrategy
import org.apache.http.protocol.HttpContext
import org.springframework.beans.factory.{DisposableBean, InitializingBean}
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.{FileAttachment, Assignment}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.spring.Wire
import com.google.api.client.auth.oauth.OAuthHmacSigner
import com.google.gdata.client.authn.oauth.{OAuthUtil, OAuthParameters}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.{DateFormats, CurrentUser}
import org.xml.sax.SAXParseException
import uk.ac.warwick.tabula.services.AutowiringOriginalityReportServiceComponent
import org.apache.commons.io.FilenameUtils._
import uk.ac.warwick.tabula.api.web.Routes
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.DateTime
import scala.util.{Failure, Success, Try}

object TurnitinLtiService {

	val AssignmentPrefix = "Assignment-"

	/**
	 * Quoted supported types are...
	 * "MS Word, Acrobat PDF, Postscript, Text, HTML, WordPerfect (WPD) and Rich Text Format".
	 */
	val validExtensions = Seq("doc", "docx", "pdf", "rtf", "txt", "wpd", "htm", "html", "ps", "odt")
	val maxFileSize = 20 * 1000 * 1000;  // 20M
	
	def validFileType(file: FileAttachment): Boolean =
		validExtensions contains getExtension(file.name).toLowerCase

	def validFileSize(file: FileAttachment): Boolean =
		file.actualDataLength < maxFileSize
	
	def validFile(file: FileAttachment): Boolean = validFileType(file) && validFileSize(file)
	
	/**
	 * ID that we should store classes under. They are per-module so we base it on the module code.
	 * This ID is stored within TurnitinLti and requests for the same ID should return the same class.
	 */
	def classIdFor(assignment: Assignment, prefix: String) = ClassId(s"$prefix-${assignment.module.code}")

	/**
	 * ID that we should store assignments under. Our assignment ID is as good an identifier as any.
	 * This ID is stored within TurnitinLti and requests for the same ID should return the same assignment.
	 */
	def assignmentIdFor(assignment: Assignment) = AssignmentId(s"${AssignmentPrefix}${assignment.id}")

	def classNameFor(assignment: Assignment) = {
		val module = assignment.module
		ClassName(s"${module.code.toUpperCase} - ${module.name}")
	}

	def assignmentNameFor(assignment: Assignment) = {
		AssignmentName(s"${assignment.name} (${assignment.academicYear.toString})")
	}
}

/**
 * Service for accessing the Turnitin LTI plagiarism API.
 */
@Service("turnitinLTIService")
class TurnitinLtiService extends Logging with DisposableBean with InitializingBean
	with AutowiringOriginalityReportServiceComponent {

	/** Turnitin LTI account id */
	@Value("${TurnitinLti.aid}") var turnitinAccountId: String = null
	/** Shared key as set up on the University of Warwick account's LTI settings */
	@Value("${TurnitinLti.key}") var sharedSecretKey: String = null

	@Value("${TurnitinLti.submitassignment.url}") var apiSubmitAssignment: String = _
	@Value("${TurnitinLti.submitpaper.url}") var apiSubmitPaperEndpoint: String = _
	@Value("${TurnitinLti.listendpoints.url}") var apiListEndpoints: String = _
	@Value("${TurnitinLti.submissiondetails.url}") var apiSubmissionDetails: String = _
	@Value("${TurnitinLti.reportlaunch.url}") var apiReportLaunch: String = _

	@Value("${turnitin.class.prefix}") var classPrefix: String =_

	@Value("${toplevel.url}") var topLevelUrl: String = _

	val userAgent = "Tabula, Coursework submission app, University of Warwick, coursework@warwick.ac.uk"

	val isoFormatter = DateFormats.IsoDateTime

	val http: Http = new Http with thread.Safety {
		override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), maxConnections, maxConnectionsPerRoute) {
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

	def submitAssignment(assignment: Assignment, user: CurrentUser): TurnitinLtiResponse = {
		doRequest(
			apiSubmitAssignment,
			Map(
				"roles" -> "Instructor",
				"resource_link_id" -> TurnitinLtiService.assignmentIdFor(assignment).value,
				"resource_link_title" -> TurnitinLtiService.assignmentNameFor(assignment).value,
				"resource_link_description" -> TurnitinLtiService.assignmentNameFor(assignment).value,
				"context_id" -> TurnitinLtiService.classIdFor(assignment, classPrefix).value,
				"context_title" -> TurnitinLtiService.classNameFor(assignment).value,
				"custom_duedate" -> isoFormatter.print(new DateTime().plusYears(2)), // default is 7 days in the future, so make it far in future
				"ext_resource_tool_placement_url" -> s"$topLevelUrl${Routes.turnitin.submitAssignmentCallback(assignment)}"
			) ++ userParams(user.email, user.firstName, user.lastName)) {
			request =>
			// expect a 302
				request >:+ {
					(headers, request) =>
						val location = headers("location").headOption
						if (!location.isDefined) throw new IllegalStateException(s"Expected a redirect url")
							request >- {
							(html) => {
								// listen to callback for actual response
								TurnitinLtiResponse.redirect(location.get)
							}
						}
				}
		}
	}

	/**
	 * Expected response xml for a successful submission
			<response>
				<status>fullsuccess</status>
				<submission_data_extract>
						Some text
			</submission_data_extract>
				<lis_result_sourcedid>[Turnitin Submission ID]</lis_result_sourcedid>
				<message>Your file has been saved successfully.</message>
			</response>
	 */
	def submitPaper(
		assignment: Assignment,	paperUrl: String, userEmail: String, attachment: FileAttachment, userFirstName: String, userLastName: String
	 ): TurnitinLtiResponse = doRequest(

		s"$apiSubmitPaperEndpoint/${assignment.turnitinId}",
		Map(
			"context_id" -> TurnitinLtiService.classIdFor(assignment, classPrefix).value,
			"context_title" -> TurnitinLtiService.classNameFor(assignment).value,
			"custom_xmlresponse" -> "1",
			// or Instructor, but must supply an author user id, whatever the parameter for that is!!!
			"roles" -> "Learner",
			// I hoped this would be the callback Turnitin uses when a paper has been processed - apparently not
			// "ext_outcomes_tool_placement_url" ->  s"$topLevelUrl/api/tunitin-outcomes",
			"custom_submission_url" -> paperUrl,
			"custom_submission_title" -> attachment.id,
			"custom_submission_filename" -> attachment.name

		)
			++ userParams(userEmail, userFirstName, userLastName) ) {
		request =>
			request >:+ {
				(headers, request) =>
					request <>  {
						(node) => {
							val response = TurnitinLtiResponse.fromXml(node)

							response
						}
					}
			}

	}

	def getSubmissionDetails(turnitinSubmissionId: String, user: CurrentUser): TurnitinLtiResponse = doRequest(
		s"$apiSubmissionDetails/$turnitinSubmissionId", Map()) {
		request =>
			request >:+ {
				(headers, request) =>
					request >- {
						(json) => {
							TurnitinLtiResponse.fromJson(json)
						}
					}
			}
	}

	def getOriginalityReportUrl(assignment: Assignment, attachment: FileAttachment, user: CurrentUser): TurnitinLtiResponse = doRequest(
		s"$apiReportLaunch/${attachment.originalityReport.turnitinId}", Map(
			"roles" -> "Instructor",
			"context_id" -> TurnitinLtiService.classIdFor(assignment, classPrefix).value,
			"context_title" -> TurnitinLtiService.classNameFor(assignment).value
		) ++ userParams(user.email, user.firstName, user.lastName),
		expectedStatusCode = Some(HttpStatus.SC_MOVED_TEMPORARILY)) {
		request =>
			request >:+ {
				(headers, request) =>
					val location = headers("location").headOption
					// TODO we could parse the html instead of throwing an exception
					/** If document cannot be found, we expect the following html
					<div id="api_errorblock">
						<h2>Sorry, we could not process your request</h2>
						<p>The requested Object Result could not be found.</p>
					</div>
						**/
					if (!location.isDefined) throw new IllegalStateException(s"Expected a redirect url")
					request >- {
						(html) => {
							TurnitinLtiResponse.redirect(location.get)
						}
					}
			}
	}

	def listEndpoints(turnitinAssignmentId: String, user: CurrentUser): TurnitinLtiResponse = doRequest(
		s"$apiListEndpoints/$turnitinAssignmentId", Map()) {
		request =>
			request >:+ {
				(headers, request) =>
					request >- {
						(json) => {
							TurnitinLtiResponse.fromJson(json)
						}
					}
			}
	}

	def ltiConformanceTestParams(
		endpoint: String,
		secret: String,
		key: String,
		givenName: String,
		familyName: String,
		email: String,
		role: String,
		mentee: String,
		customParams: String,
		tool_consumer_info_version: String,
		assignment: Assignment,
		user: CurrentUser ) = {

		val signedParams = getSignedParamsWithKey(
			Map(
				"lis_result_sourcedid" -> assignment.id,
				"lis_outcome_service_url" -> s"$topLevelUrl/api/v1/turnitin/turnitin-lti-outcomes/assignment/${assignment.id}",
				"resource_link_id" -> assignment.id,
				"roles" -> role,
				"role_scope_mentor" -> mentee,
				"context_title" -> assignment.module.name,
				"context_id" -> assignment.module.code,
				"context_label" -> "A label for this context",
				"resource_link_title" -> assignment.name,
				"resource_link_description" -> "A description for this resource link",
				"tool_consumer_info_version" -> tool_consumer_info_version,

				// custom - custom params should be prefixed with _custom
				"custom_debug" -> "true"

			)  ++ userParams(email, givenName, familyName) ++ customParamsForTesting(customParams, email) filter(p => p._2.nonEmpty),
			endpoint, Some(secret), key
		)

		logger.debug("doRequest: " + signedParams)

		signedParams

	}

	def customParamsForTesting(customParams: String, email: String): Map[String, String] = {
		/**
		 * The LTI 1 specification forces all characters in key names to lower case and
		 * maps anything that is not a number or letter to an underscore.
		 * In LTI 2 it is best practice to send the parameter under both names, if different.
		 */

		val paramsList = customParams.split("\n").toList

		val map =	paramsList.map (
				p => (s"custom_${p.substring(0, p.indexOf("=")).replaceAll("[^a-zA-Z0-9]", "_").toLowerCase}"
					-> p.substring(p.indexOf("=")+1).replace("$User.id", email).replace("$User.username", email).replace("$ToolConsumerProfile.url", topLevelUrl)
					.filter(_ >= ' '))) // strip out carriage returns etc.
				.toMap
		map
}

	def userParams(email: String, firstName: String, lastName: String): Map[String, String] = {
		Map(
			// according to the spec, need lis_person_name_full or both lis_person_name_family and lis_person_name_given.
			"user_id" -> email,
			"lis_person_contact_email_primary" -> email,
			"lis_person_name_given" -> firstName,
			"lis_person_name_family" -> lastName)
	}

	def commonParameters = Map(
		"lti_message_type" -> "basic-lti-launch-request",
		"lti_version" -> "LTI-1p0",
		"launch_presentation_locale" -> "en-GB",
		"tool_consumer_info_product_family_code" -> "Tabula"
	)

	def doRequestForLtiTesting(
		endpoint: String, secret: String, key: String, params: Map[String, String], expectedStatusCode: Option[Int] = None
	) (transform: Request => Handler[TurnitinLtiResponse]): TurnitinLtiResponse = {

		val signedParams = getSignedParamsWithKey(params, endpoint, Some(secret), key)

		logger.debug("doRequest: " + signedParams)

		val req = (url(endpoint) <:< Map()).POST << signedParams

		try {
			if (expectedStatusCode.isDefined){
				Try(http.when(_==expectedStatusCode.get)(transform(req))) match {
					case Success(response) => response
					case Failure(StatusCode(code, contents)) =>
						logger.warn(s"Not expected http status code")
						new TurnitinLtiResponse(false, statusMessage = Some("Unexpected HTTP status code"), responseCode = Some(code))
					case _ =>
						new TurnitinLtiResponse(false, statusMessage = Some("Unexpected HTTP status code"))
				}
			} else http.x(transform(req))
		} catch {
				case e: IOException =>
					logger.error("Exception contacting provider", e)
					new TurnitinLtiResponse(false, statusMessage = Some(e.getMessage))
				case e: SAXParseException =>
					logger.error("Unexpected response from provider", e)
					new TurnitinLtiResponse(false, statusMessage = Some(e.getMessage))
		}
	}

	def doRequest(
	endpoint: String, params: Map[String, String], expectedStatusCode: Option[Int] = None
	) (transform: Request => Handler[TurnitinLtiResponse]): TurnitinLtiResponse = {

		val signedParams = getSignedParams(params, endpoint, None)

		val req = (url(endpoint) <:< Map()).POST << signedParams

		logger.debug("doRequest: " + signedParams)
		try {
			if (expectedStatusCode.isDefined){
				Try(http.when(_==expectedStatusCode.get)(transform(req))) match {
					case Success(response) => response
					case Failure(StatusCode(code, contents)) =>
						logger.warn(s"Not expected http status code")
						new TurnitinLtiResponse(false, statusMessage = Some("Unexpected HTTP status code"), responseCode = Some(code))
					case _ =>
						new TurnitinLtiResponse(false, statusMessage = Some("Unexpected HTTP status code"))
				}
			} else http.x(transform(req))
		} catch {
				case e: IOException =>
					logger.error("Exception contacting Turnitin", e)
					new TurnitinLtiResponse(false, statusMessage = Some(e.getMessage))
				case e: java.lang.Exception =>
					logger.error("Some other exception", e)
					new TurnitinLtiResponse(false, statusMessage = Some(e.getMessage))
		}
	}

	def getSignedParams(params: Map[String, String], endpoint: String, optionalSecret: Option[String]): Map[String, String] = {
		val hmacSigner = new OAuthHmacSigner()
		hmacSigner.clientSharedSecret = optionalSecret.getOrElse(sharedSecretKey)

		val oauthparams = new OAuthParameters()
		oauthparams.setOAuthConsumerKey(turnitinAccountId)
		oauthparams.setOAuthNonce(OAuthUtil.getNonce)
		oauthparams.setOAuthTimestamp(OAuthUtil.getTimestamp)
		oauthparams.setOAuthSignatureMethod("HMAC-SHA1")
		oauthparams.setOAuthCallback("about:blank")
		oauthparams.addCustomBaseParameter("oauth_version", "1.0")

		val allParams = commonParameters ++ params ++ oauthparams.getBaseParameters.asScala ++ oauthparams.getExtraParameters.asScala

		val signatureBaseString = OAuthUtil.getSignatureBaseString(endpoint, "POST", allParams.asJava)
		val signature = hmacSigner.computeSignature(signatureBaseString)

		oauthparams.addCustomBaseParameter("oauth_signature", signature)
		val allParamsIncludingSignature = commonParameters ++ params ++ oauthparams.getBaseParameters.asScala ++ oauthparams.getExtraParameters.asScala
		allParamsIncludingSignature
	}

	def getSignedParamsWithKey(params: Map[String, String], endpoint: String, optionalSecret: Option[String], key: String): Map[String, String] = {
		val hmacSigner = new OAuthHmacSigner()
		hmacSigner.clientSharedSecret = optionalSecret.getOrElse(sharedSecretKey)

		val oauthparams = new OAuthParameters()
		oauthparams.setOAuthConsumerKey(key)
		oauthparams.setOAuthNonce(OAuthUtil.getNonce)
		oauthparams.setOAuthTimestamp(OAuthUtil.getTimestamp)
		oauthparams.setOAuthSignatureMethod("HMAC-SHA1")
		oauthparams.setOAuthCallback("about:blank")

		val allParams = commonParameters ++ params ++ oauthparams.getBaseParameters.asScala ++ oauthparams.getExtraParameters.asScala

		val signatureBaseString = OAuthUtil.getSignatureBaseString(endpoint, "POST", allParams.asJava)
		val signature = hmacSigner.computeSignature(signatureBaseString)

		oauthparams.addCustomBaseParameter("oauth_signature", signature)
		val allParamsIncludingSignature = commonParameters ++ params ++ oauthparams.getBaseParameters.asScala ++ oauthparams.getExtraParameters.asScala
		allParamsIncludingSignature
	}
}

trait TurnitinLtiServiceComponent {
	def turnitinLtiService: TurnitinLtiService
}

trait AutowiringTurnitinLtiServiceComponent extends TurnitinLtiServiceComponent {
	var turnitinLtiService = Wire[TurnitinLtiService]
}

case class ClassName(value: String)

case class ClassId(value: String)

case class AssignmentName(value: String)

case class AssignmentId(value: String)
