package uk.ac.warwick.tabula.services.turnitinlti

import dispatch.classic._
import dispatch.classic.mime.Mime._
import java.io.IOException
import org.apache.commons.codec.digest.DigestUtils
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.content.FileBody
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.Products._
import org.xml.sax.SAXParseException
import scala.collection.JavaConverters._
import com.google.gdata.client.authn.oauth.{OAuthParameters, OAuthUtil}

import com.google.api.client.auth.oauth.OAuthHmacSigner

/**
 * Acquired from a call to Turnitin.login(), this will call Turnitin methods as a particular
 * user.
 */
class TurnitinLtiSession(turnitinLti: TurnitinLti, val sessionId: String) extends TurnitinLtiMethods with Logging {
	
	/**
	 * The API calls are split out into TurnitinMethods - the body of this class mostly contains
	 * the supporting methods for generating the valid signed Turnitin requests. The API methods
	 * then call the doRequest() function with whatever parameters.
	 */

	import TurnitinLtiDates._

	// Some parameters are not included in the MD5 signature calculation.
	val excludeFromMd5 = Seq(
		"dtend", "create_session", "session-id", "src", "apilang",
		"exclude_biblio", "exclude_quoted", "exclude_type", "exclude_value"
	)

	// These are overriden within Turnitin.login().
	var userEmail = ""
	var userFirstName = ""
	var userLastName = ""
	var userId = ""

	private val http = turnitinLti.http
	private def diagnostic = turnitinLti.diagnostic
	def apiEndpoint = turnitinLti.apiEndpoint

	/**
	 * All API requests call the same URL and require the same MD5
	 * signature parameter.
	 *
	 * If you start getting an "MD5 NOT AUTHENTICATED" on an API method you've
	 * changed, it's usually because it doesn't recognise one of the parameters.
	 * We MD5 on all parameters but the server will only MD5 on the parameters
	 * it recognises, hence the discrepency. There is no way to know which parameters
	 * that Turnitin cares about. There is no list in the docs. What fun!
	 */
	def doRequest(
		pdata: Option[FileData], // optional file to put in "pdata" parameter
		params: (String, String)*): TurnitinLtiResponse = {

		val req = getRequest(pdata, params:_*)

		val request: Handler[TurnitinLtiResponse] =
			req >:+ { (headers, req) =>
				val location = headers("location").headOption
				if (location.isDefined) req >- { (text) => TurnitinLtiResponse.redirect(location.get) }
				else req >- { (text) => TurnitinLtiResponse.fromDiagnostic(text) }
//				else if (turnitinLti.diagnostic) req >- { (text) => TurnitinLtiResponse.fromDiagnostic(text) }
//				else req <> { (node) => TurnitinLtiResponse.fromXml(node) }
			}

		try {
			val response = http.x(request)
			logger.debug("Response: " + response)
			response
		} catch {
			case e: IOException => {
				logger.error("Exception contacting Turnitin", e)
				new TurnitinLtiResponse(code = 9000, diagnostic = Some(e.getMessage))
			}
			case e: SAXParseException => {
				logger.error("Unexpected response from Turnitin", e)
				new TurnitinLtiResponse(code = 9001, diagnostic = Some (e.getMessage))
			}
		}
	}
	
	def getRequest(
		pdata: Option[FileData], // optional file to put in "pdata" parameter
		params: (String, String)*) = {
		
		val fullParameters = calculateParameters(params:_*)
		val postWithParams = turnitinLti.endpoint.POST << fullParameters
		val req = addPdata(pdata, postWithParams)
		logger.debug("doRequest: " + fullParameters)
		req
	}
	
	def calculateParameters(params: (String, String)*) = {
		val parameters = (commonParameters ++ params).filterNot(nullValue)
		(parameters + md5hexparam(parameters))
	}

	/**
	 * Makes a request as in doRequest, but leaves the response handling to you, via
	 * the transform function.
	 */
	def doRequestAdvanced(
		pdata: Option[FileData], // optional file to put in "pdata" parameter
		params: (String, String)*) // POST parameters
		(transform: Request => Handler[TurnitinLtiResponse]): TurnitinLtiResponse = {

		val hmacSigner = new OAuthHmacSigner()
		val secret = "ir00nag3"
		hmacSigner.clientSharedSecret = secret

		val oauthparams = new OAuthParameters()
		oauthparams.setOAuthConsumerKey("69622")
		oauthparams.setOAuthNonce(OAuthUtil.getNonce())
		oauthparams.setOAuthTimestamp(OAuthUtil.getTimestamp())
		oauthparams.setOAuthSignatureMethod("HMAC-SHA1")

		oauthparams.addCustomBaseParameter("oauth_version", "1.0")

		val allParams = commonParameters ++ params ++ oauthparams.getBaseParameters.asScala

		val signatureBaseString = OAuthUtil.getSignatureBaseString(apiEndpoint, "POST", allParams.asJava)
		val signature = hmacSigner.computeSignature(signatureBaseString)

		oauthparams.addCustomBaseParameter("oauth_signature", signature)
		val allParamsIncludingSignature = commonParameters ++ params ++ oauthparams.getBaseParameters.asScala
		val postWithParams = turnitinLti.endpoint.POST << (allParamsIncludingSignature)
		val req = addPdata(pdata, postWithParams)

		logger.info("doRequest: " + allParamsIncludingSignature)

		try {
			val resp = http.x(
				req >- {
					(text) => TurnitinLtiResponse.fromDiagnostic(text)
				})

			//				if (diagnostic) req >- { (text) => TurnitinLtiResponse.fromDiagnostic(text) }
//				else transform(req)
// )
			resp
		} catch {
			case e: IOException => {
				logger.error("Exception contacting Turnitin", e)
				new TurnitinLtiResponse(code = 9000, diagnostic = Some(e.getMessage))
			}
			case e: java.lang.Exception => {
				logger.error("Some other exception", e)
				new TurnitinLtiResponse(code = 9000, diagnostic = Some(e.getMessage))
			}
		}
	}

	/**
	 * Returns either the request with a file added on, or the original
	 * request if there's no file to add.
	 * It's important to pass in the file and not a stream of the file because the production Turnitin
	 * server will explode with a confusing error if you don't provide a Content-Length header.
	 */
	def addPdata(file: Option[FileData], req: Request) = file match {
		case Some(data) if data.file != null => req.add("pdata", new FileBody(data.file, ContentType.APPLICATION_OCTET_STREAM, data.name))
		case _ => req
	}

	/**
	 * Parameters that we need in every request.
	 */
	def commonParameters = Map(

		"lti_message_type" -> "basic-lti-launch-request",
//		"lti_version" -> "LTI-2p0",
		"lti_version" -> "LTI-1p0",

	  "roles" -> "Instructor",

		"diagnostic" -> (if (diagnostic) "1" else "0"),
		"gmtime" -> gmtTimestamp,
		"encrypt" -> "0",
		"aid" -> turnitinLti.aid,
		"fcmd" -> "2",
		"uid" -> "abdef",
		"uem" -> userEmail,
		"ufn" -> userFirstName,
		"uln" -> userLastName,
		"utp" -> "2",
		"dis" -> "1", // disable emails
		"src" -> turnitinLti.integrationId) ++ (subAccountParameter) ++ (sessionIdParameter)

	/** Optional sub-account ID */ 
	private def subAccountParameter: Map[String, String] = 
		if (turnitinLti.said == null || turnitinLti.said.isEmpty)
			Map.empty
		else 
			Map("said" -> turnitinLti.said)
			
	/** Optional session ID */
	private def sessionIdParameter: Map[String, String] = {
		if (sessionId == null) 
			Map.empty
		else 
			Map("session-id" -> sessionId)
	}

	/** The md5 signature to add to the request parameter map. */
	def md5hexparam(map: Map[String, String]) = ("md5" -> md5hex(map))

	/**
	 * Sort parameters by key, concatenate all the values with
	 * the shared key and MD5hex that.
	 */
	def md5hex(params: Map[String, String]) = {
		val filteredParams = params.filterKeys(!excludeFromMd5.contains(_)).toSeq
		val sortedParams = filteredParams.sortBy(toKey) // sort by key (left part of Pair)
		val sortedValues = sortedParams.map(toValue).mkString // map to value (right part of Pair)
		DigestUtils.md5Hex(sortedValues + turnitinLti.sharedSecretKey)
	}

}
