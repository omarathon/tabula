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
import org.xml.sax.SAXParseException

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

	val userAgent = "Tabula, Cursework submission app, University of Warwick, coursework@warwick.ac.uk"

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

	override def afterPropertiesSet {

	}

	def getOriginalityReport(turnitinSubmissionId: String, user: CurrentUser): TurnitinLtiResponse = {

		val response = doRequest(

			s"${apiReportLaunch}/${turnitinSubmissionId}", Map(
//						s"${apiReportLaunch}/${turnitinSubmissionId}/instructor", Map(
				"roles" -> "Instructor",
				"context_id" -> "TestModule-po206",
				"context_title" -> "PO206 - Politics in the United Kingdom"

			) ++ userParams(user))

		if (response.success) logger.info ("success!")
		else logger.info("failed!")
		response
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
//								else FailedresolveError(response)
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
								val response = TurnitinLtiResponse.fromJson(json)
//								if (response.success) response.submissionInfo
								//								else FailedresolveError(response)
								response
							}
						}
				}
		}

	def submitAssignment(assignmentId: AssignmentId, assignmentName: AssignmentName,
											 classId: ClassId, className: ClassName, user: CurrentUser): TurnitinLtiResponse = {
		doRequestAdvanced(
			apiSubmitAssignment,
			Map(
			"roles" -> "Instructor",
			"resource_link_id" -> assignmentId.value,
			"resource_link_title" -> assignmentName.value,
			"resource_link_description" -> assignmentName.value,
			"context_id" -> classId.value,
			"context_title" -> className.value,
			"ext_resource_tool_placement_url" -> s"$topLevelUrl/api/turnitin-response",
			"ext_outcomes_tool_placement_url" -> s"$topLevelUrl/api/tunitin-outcomes") ++ userParams(user)) {
			request =>
				request >:+ {
					(headers, request) =>
						//request >-  {
						request >- {
							(html) => {
								// listen to callback for actual response
								// this response should be a redirect
								val response = TurnitinLtiResponse.fromHtml(html)
//								if (response.success) response.submissionInfo
								//								else FailedresolveError(response)

								if (response.success) logger.info("success!")
								else logger.info("failed!")

								response
							}
						}
				}
		}
	}
	def submitPaper(turnitinAssignmentId: String, paperUrl: String, user: CurrentUser): TurnitinLtiResponse = doRequestAdvanced(
		apiSubmitPaperEndpoint + "/" + turnitinAssignmentId,

		// looks like it needs to be the student, not the instructor, else status: failure: "You are not authorized to access this resource."
		Map(

		// if you don't add context_id, maybe context_title, then status: failure: "User not found for this account and API product."
		"context_id" -> "TestModule-po206",
		"context_title" -> "PO206 - Politics in the United Kingdom",

		"custom_xmlresponse" -> "1",

		// or Instructor, but must supply an author user id
		"roles" -> "Learner",
		"custom_submission_url" -> paperUrl,
		"custom_submission_title" -> "Paper title",
		"custom_submission_filename" -> "test.doc",

		"ext_outcomes_tool_placement_url" -> s"$topLevelUrl/api/turnitin-response",
		"ext_outcome_tool_placement_url" -> s"$topLevelUrl/api/turnitin-response",
		"lis_outcome_service_url" -> s"$topLevelUrl/api/tunitin-response")
	  ++ userParams(user) ) {
		request =>
			request >:+ {
				(headers, request) =>
					logger.info(headers.toString)

					//request >-  {
					//val b = request <>  {
					request <>  {
						(node) => {
							// listen to callback for actual response
							val response = TurnitinLtiResponse.fromXml(node)
//							if (response.success) response.submissionInfo
							//								else FailedresolveError(response)
							if (response.success) logger.info("success")
							else logger.info("failure")
							response
						}
					}
			}
	}


	def userParams(user: CurrentUser): Map[String, String] = {
		Map(
			"user_id" -> user.email,
			"lis_person_contact_email_primary" -> user.email,
			"lis_person_contact_name_given" -> user.firstName,
			"lis_person_contact_name_family" -> user.lastName)
	}

	/**
	 * Parameters that we need in every request.
	 */
	def commonParameters = Map(
		"lti_message_type" -> "basic-lti-launch-request",
		"lti_version" -> "LTI-1p0",
		"User-Agent" -> userAgent
		)

	def doRequest(endpoint: String, params: Map[String, String]): TurnitinLtiResponse = {

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

		//		val postWithParams =	(url(endpoint) <:< Map()).POST << (allParamsIncludingSignature)
		//
		//		val req = postWithParams

		val req =	(url(endpoint) <:< Map()).POST << (allParamsIncludingSignature)



		val request: Handler[TurnitinLtiResponse] =
			req >:+ { (headers, req) =>
				val location = headers("location").headOption
				if (location.isDefined) req >- { (text) => TurnitinLtiResponse.redirect(location.get) }
//				else if (turnitin.diagnostic) req >- { (text) => TurnitinResponse.fromDiagnostic(text) }
				//else req <> { (node) => TurnitinLtiResponse.fromHtml(node) }
				else req >- { (html) => TurnitinLtiResponse.fromHtml(html) }
			}

		try {
			val response = http.x(request)
			logger.debug("Response: " + response)
			response
		} catch {
			case e: IOException => {
				logger.error("Exception contacting Turnitin", e)
				new TurnitinLtiResponse(false, diagnostic = Some(e.getMessage))
			}
			case e: SAXParseException => {
				logger.error("Unexpected response from Turnitin", e)
				new TurnitinLtiResponse(false, diagnostic = Some (e.getMessage))
			}
		}
	}


	def doRequestAdvanced(endpoint: String, params: Map[String, String])
	 (transform: Request => Handler[TurnitinLtiResponse]): TurnitinLtiResponse = {

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

//		val postWithParams =	(url(endpoint) <:< Map()).POST << (allParamsIncludingSignature)
//
//		val req = postWithParams

		val req =	(url(endpoint) <:< Map()).POST << (allParamsIncludingSignature)

		logger.info("doRequest: " + allParamsIncludingSignature)

		try {
			http.x(
//				if (diagnostic) req >- { (text) => TurnitinLtiResponse.fromDiagnostic(text) }
//				else transform(req))
				transform(req))
		} catch {
			case e: IOException => {
				logger.error("Exception contacting Turnitin", e)
				//new TurnitinLtiResponse(code = 9000, diagnostic = Some(e.getMessage))
				new TurnitinLtiResponse(false, statusMessage = Some(e.getMessage))
			}
			case e: java.lang.Exception => {
				logger.error("Some other exception", e)
//				new TurnitinLtiResponse(code = 9000, diagnostic = Some(e.getMessage))
				new TurnitinLtiResponse(false, statusMessage = Some(e.getMessage))
			}
		}
	}
}

trait TurnitinLtiServiceComponent {
	def turnitinLtiService: TurnitinLtiService
}

trait AutowiringTurnitinLtiServiceComponent extends TurnitinLtiServiceComponent {
	var turnitinLtiService = Wire[TurnitinLtiService]
}
