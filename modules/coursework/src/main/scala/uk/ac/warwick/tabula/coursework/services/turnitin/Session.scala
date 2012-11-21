package uk.ac.warwick.tabula.coursework.services.turnitin

import dispatch._
import dispatch.mime.Mime._
import java.io.FileInputStream
import org.apache.commons.codec.digest.DigestUtils
import org.apache.http.entity.mime.content.FileBody
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.Products._

/**
 * Acquired from a call to Turnitin.login(), this will call Turnitin methods as a particular
 * user.
 */
class Session(turnitin: Turnitin, val sessionId: String) extends TurnitinMethods with Logging {
	
	/**
	 * The API calls are split out into TurnitinMethods - the body of this class mostly contains
	 * the supporting methods for generating the valid signed Turnitin requests. The API methods
	 * then call the doRequest() function with whatever parameters.
	 */

	import TurnitinDates._

	// Some parameters are not included in the MD5 signature calculation.
	val excludeFromMd5 = Seq("dtend", "create_session", "session-id", "src", "apilang")

	// These are overriden within Turnitin.login().
	var userEmail = ""
	var userFirstName = ""
	var userLastName = ""
	var userId = ""

	private val http = turnitin.http
	private def diagnostic = turnitin.diagnostic
	def apiEndpoint = turnitin.apiEndpoint

	/**
	 * All API requests call the same URL and require the same MD5
	 * signature parameter.
	 *
	 * If you start getting an "MD5 NOT AUTHENTICATED" on an API method you've
	 * changed, it's usually because it doesn't recognise one of the parameters.
	 * We MD5 on all parameters but the server will only MD5 on the parameters
	 * it recognises, hence the discrepency.
	 */
	def doRequest(
		functionId: String, // API function ID (defined in TurnitinMethods object)
		pdata: Option[FileData], // optional file to put in "pdata" parameter
		params: Pair[String, String]*): TurnitinResponse = {

		val req = getRequest(functionId, pdata, params:_*)

		val request: Handler[TurnitinResponse] =
			req >:+ { (headers, req) =>
				val location = headers("location").headOption
				if (location.isDefined) req >- { (text) => TurnitinResponse.redirect(location.get) }
				else if (turnitin.diagnostic) req >- { (text) => TurnitinResponse.fromDiagnostic(text) }
				else req <> { (node) => TurnitinResponse.fromXml(node) }
			}
		val response = http.x(request)
		logger.debug("Response: " + response)
		response
	}
	
	def getRequest(
		functionId: String, // API function ID (defined in TurnitinMethods object)
		pdata: Option[FileData], // optional file to put in "pdata" parameter
		params: Pair[String, String]*) = {
		
		val fullParameters = calculateParameters(functionId, params:_*)
		val postWithParams = turnitin.endpoint.POST << fullParameters
		val req = addPdata(pdata, postWithParams)
		logger.debug("doRequest: " + fullParameters)
		req
	}
	
	def calculateParameters(functionId: String, params: Pair[String, String]*) = {
		val parameters = (Map("fid" -> functionId) ++ commonParameters ++ params).filterNot(nullValue)
		(parameters + md5hexparam(parameters))
	}

	/**
	 * Makes a request as in doRequest, but leaves the response handling to you, via
	 * the transform function.
	 */
	def doRequestAdvanced(
		functionId: String, // API function ID
		pdata: Option[FileData], // optional file to put in "pdata" parameter
		params: Pair[String, String]*) // POST parameters
		(transform: Request => Handler[TurnitinResponse]): TurnitinResponse = {

		val parameters = Map("fid" -> functionId) ++ commonParameters ++ params
		val postWithParams = turnitin.endpoint.POST << (parameters + md5hexparam(parameters))
		val req = addPdata(pdata, postWithParams)

		logger.debug("doRequest: " + parameters)

		http.x(
			if (diagnostic) req >- { (text) => TurnitinResponse.fromDiagnostic(text) }
			else transform(req))
	}

	/**
	 * Returns either the request with a file added on, or the original
	 * request if there's no file to add.
	 * It's important to pass in the file and not a stream of the file because the production Turnitin
	 * server will explode with a confusing error if you don't provide a Content-Length header.
	 */
	def addPdata(file: Option[FileData], req: Request) = file match {
		case Some(data) => req.add("pdata", new FileBody(data.file, data.name, "application/octet-stream", null))
		case None => req
	}

	/**
	 * Parameters that we need in every request.
	 */
	def commonParameters = Map(
		"diagnostic" -> (if (diagnostic) "1" else "0"),
		"gmtime" -> gmtTimestamp,
		"encrypt" -> "0",
		"aid" -> turnitin.aid,
		"fcmd" -> "2",
		"uid" -> userId,
		"uem" -> userEmail,
		"ufn" -> userFirstName,
		"uln" -> userLastName,
		"utp" -> "2",
		"dis" -> "1", // disable emails
		"src" -> turnitin.integrationId) ++ (subAccountParameter) ++ (sessionIdParameter)

	/** Optional sub-account ID */ 
	private def subAccountParameter: Map[String, String] = 
		if (turnitin.said == null || turnitin.said.isEmpty) 
			Map.empty
		else 
			Map("said" -> turnitin.said)
			
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
		val sortedJoinedParams = params.filterKeys(!excludeFromMd5.contains(_)).toSeq
			.sortBy(toKey) // sort by key (left part of Pair)
			.map(toValue) // map to value (right part of Pair)
			.mkString
		DigestUtils.md5Hex(sortedJoinedParams + turnitin.sharedSecretKey)
	}

}
