package uk.ac.warwick.tabula.services.turnitinlti

import java.io.IOException

import dispatch.classic.Request.toRequestVerbs
import dispatch.classic._
import dispatch.classic.thread.ThreadSafeHttpClient
import org.apache.http.{HttpRequest, HttpResponse}
import org.apache.http.client.params.{ClientPNames, CookiePolicy}
import org.apache.http.impl.client.DefaultRedirectStrategy
import org.apache.http.protocol.HttpContext
import org.springframework.beans.factory.{DisposableBean, InitializingBean}
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.spring.Wire
import com.google.api.client.auth.oauth.OAuthHmacSigner
import com.google.gdata.client.authn.oauth.{OAuthUtil, OAuthParameters}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.CurrentUser

object TurnitinLtiService {

	/**
	 * ID that we should store classes under. They are per-module so we base it on the module code.
	 * This ID is stored within TurnitinLti and requests for the same ID should return the same class.
	 */
	def classIdFor(assignment: Assignment, prefix: String) = ClassId(prefix + "-" + assignment.module.code)

	/**
	 * ID that we should store assignments under. Our assignment ID is as good an identifier as any.
	 * This ID is stored within TurnitinLti and requests for the same ID should return the same assignment.
	 */
	def assignmentIdFor(assignment: Assignment) = AssignmentId("Assignment-" + assignment.id)

	def classNameFor(assignment: Assignment) = {
		val module = assignment.module
		ClassName(module.code.toUpperCase + " - " + module.name)
	}

	def assignmentNameFor(assignment: Assignment) = {
		AssignmentName(assignment.name + " (" + assignment.academicYear.toString + ")")
	}
}

/**
 * Service for accessing the Turnitin LTI plagiarism API.
 */
@Service("turnitinLTIService")
class TurnitinLtiService extends Logging with DisposableBean with InitializingBean {

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

	val http: Http = new Http with thread.Safety {
		override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), maxConnections, maxConnectionsPerRoute) {
			setRedirectStrategy(new DefaultRedirectStrategy {
				override def isRedirected(req: HttpRequest, res: HttpResponse, ctx: HttpContext) = false
			})
			getParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
		}
	}

	override def destroy {
		http.shutdown()
	}

	override def afterPropertiesSet {}

	def submitAssignment(assignment: Assignment, user: CurrentUser): TurnitinLtiResponse = {
		doRequestAdvanced(
			apiSubmitAssignment,
			Map(
				"roles" -> "Instructor",
				"resource_link_id" -> TurnitinLtiService.assignmentIdFor(assignment).value,
				"resource_link_title" -> TurnitinLtiService.assignmentNameFor(assignment).value,
				"resource_link_description" -> TurnitinLtiService.assignmentNameFor(assignment).value,
				"context_id" -> TurnitinLtiService.classIdFor(assignment, classPrefix).value,
				"context_title" -> TurnitinLtiService.classNameFor(assignment).value,
				// TODO callback url should be a property
				"ext_resource_tool_placement_url" -> s"$topLevelUrl/api/turnitin-response",
				"ext_outcomes_tool_placement_url" -> s"$topLevelUrl/api/tunitin-outcomes") ++ userParams(user.email, user.firstName, user.lastName)) {
			request =>
			// actually, expect a 302
				request >:+ {
					(headers, request) =>
						request >- {
							(html) => {
								// listen to callback for actual response
								val response = TurnitinLtiResponse.fromHtml(html.contains("Moved"), html)
								if (!response.success) logger.error("Unexpected response submitting assignment to Turnitin")
								response
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
	def submitPaper(assignment: Assignment,	paperUrl: String,	userEmail: String, filename: String,
									userFirstName: String, userLastName: String): TurnitinLtiResponse = doRequestAdvanced(

		apiSubmitPaperEndpoint + "/" + assignment.turnitinId,
		Map(
			"context_id" -> TurnitinLtiService.classIdFor(assignment, classPrefix).value,
			"context_title" -> TurnitinLtiService.classNameFor(assignment).value,
			"custom_xmlresponse" -> "1",

			// or Instructor, but must supply an author user id, whatever the parameter for that is!!!
			"roles" -> "Learner",
			"custom_submission_url" -> paperUrl,
			"custom_submission_title" -> filename,
			"custom_submission_filename" -> filename

		)
			++ userParams(userEmail, userFirstName, userLastName) ) {
		request =>
			request >:+ {
				(headers, request) =>
					logger.info(headers.toString)
					request <>  {
						(node) => {
							val response = TurnitinLtiResponse.fromXml(node)
							if (response.success) logger.info("turnitin submission id: " + response.turnitinSubmissionId())
							else logger.info("failure")
							response
						}
					}
			}

	}

	def getSubmissionDetails(turnitinSubmissionId: String, user: CurrentUser): TurnitinLtiResponse = doRequestAdvanced(
		s"${apiSubmissionDetails}/${turnitinSubmissionId}", Map()) {
		request =>
			request >:+ {
				(headers, request) =>
					request >- {
						(json) => {
							val response = TurnitinLtiResponse.fromJson(json)
							if (response.success) response.submissionInfo
							response
						}
					}
			}
	}

	def listEndpoints(turnitinAssignmentId: String, user: CurrentUser): TurnitinLtiResponse = doRequestAdvanced(
		apiListEndpoints + "/" + turnitinAssignmentId, Map()) {
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

	def userParams(email: String, firstName: String, lastName: String): Map[String, String] = {
		Map(
			"user_id" -> email,
			"lis_person_contact_email_primary" -> email,
			"lis_person_contact_name_given" -> firstName,
			"lis_person_contact_name_family" -> lastName)
	}

	def commonParameters = Map(
		"lti_message_type" -> "basic-lti-launch-request",
		"lti_version" -> "LTI-1p0",
		"User-Agent" -> userAgent
	)

	def doRequestAdvanced(endpoint: String, params: Map[String, String])
											 (transform: Request => Handler[TurnitinLtiResponse]): TurnitinLtiResponse = {

		val signedParams = getSignedParams(params, endpoint)

		val req =	(url(endpoint) <:< Map()).POST << signedParams

		logger.info("doRequest: " + signedParams)

		try {
			http.x(transform(req))
		} catch {
			case e: IOException => {
				logger.error("Exception contacting Turnitin", e)
				new TurnitinLtiResponse(false, statusMessage = Some(e.getMessage))
			}
			case e: java.lang.Exception => {
				logger.error("Some other exception", e)
				new TurnitinLtiResponse(false, statusMessage = Some(e.getMessage))
			}
		}
	}

	def getSignedParams(params: Map[String, String], endpoint: String): Map[String, String] = {
		val hmacSigner = new OAuthHmacSigner()
		hmacSigner.clientSharedSecret = sharedSecretKey

		val oauthparams = new OAuthParameters()
		oauthparams.setOAuthConsumerKey(turnitinAccountId)
		oauthparams.setOAuthNonce(OAuthUtil.getNonce())
		oauthparams.setOAuthTimestamp(OAuthUtil.getTimestamp())
		oauthparams.setOAuthSignatureMethod("HMAC-SHA1")

		oauthparams.addCustomBaseParameter("oauth_version", "1.0")

		val allParams = commonParameters ++ params ++ oauthparams.getBaseParameters.asScala

		val signatureBaseString = OAuthUtil.getSignatureBaseString(endpoint, "POST", allParams.asJava)
		val signature = hmacSigner.computeSignature(signatureBaseString)

		oauthparams.addCustomBaseParameter("oauth_signature", signature)
		val allParamsIncludingSignature = commonParameters ++ params ++ oauthparams.getBaseParameters.asScala
		allParamsIncludingSignature
	}
}

trait TurnitinLtiServiceComponent {
	def turnitinLtiService: TurnitinLtiService
}

trait AutowiringTurnitinLtiServiceComponent extends TurnitinLtiServiceComponent {
	var turnitinLtiService = Wire[TurnitinLtiService]
}
