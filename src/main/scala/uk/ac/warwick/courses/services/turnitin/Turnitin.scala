package uk.ac.warwick.courses.services.turnitin

import uk.ac.warwick.courses.helpers.Logging
import dispatch._
import dispatch.mime.Mime._
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.format.DateTimeFormatter
import org.joda.time.DateTime
import org.apache.commons.codec.digest.DigestUtils
import scala.xml.NodeSeq
import scala.xml.Elem
import java.io.File
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Service
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import java.io.FileInputStream
import uk.ac.warwick.courses.data.model.FileAttachment
import org.apache.commons.io.FilenameUtils
import org.apache.http.impl.client.DefaultRedirectStrategy
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.protocol.HttpContext

case class FileData(val file: File, val name: String)

object Turnitin {
	/**
	 * Quoted supported types are...
	 * "MS Word, Acrobat PDF, Postscript, Text, HTML, WordPerfect (WPD) and Rich Text Format".
	 */
	val validExtensions = Seq("doc", "docx", "pdf", "rtf", "txt", "wpd", "htm", "html", "ps")

	def validFileType(file: FileAttachment): Boolean =
		Turnitin.validExtensions contains FilenameUtils.getExtension(file.name).toLowerCase
}

/**
 * Service for accessing the Turnitin plagiarism API.
 *
 * You call login() first with a user's details, which if successful will give you a 
 * Session that has methods that will be called as that user. When done you can call
 * logout() to end the session.
 */
@Service
class Turnitin extends Logging with DisposableBean with InitializingBean {


	/** The top level account ID (usually for University of Warwick account) */
	@Value("${turnitin.aid}") var aid: String = null
	/** Sub-account ID underneath University of Warwick */
	@Value("${turnitin.said}") var said: String = null
	/** Shared key as set up on the University of Warwick account's Open API settings */
	@Value("${turnitin.key}") var sharedSecretKey: String = null

	@Value("${turnitin.url}") var apiEndpoint: String = _

	// Warwick's API version
	@Value("${turnitin.integration}") var integrationId: String = _

	/**
	 * If this is set to true, responses are returned with HTML debug info,
	 * and also it doesn't make any changes - the server just lets you know whether
	 * your request looks okay.
	 */
	var diagnostic = false

	val userAgent = "Coursework submission app, University of Warwick, coursework@warwick.ac.uk"

	// URL to call for all requests.
	lazy val endpoint = url(apiEndpoint) <:< Map("User-Agent" -> userAgent)

	val http: Http = new Http with thread.Safety {
		override def make_client = {
			val client = new ConfiguredHttpClient(new Http.CurrentCredentials(None))
			client.setRedirectStrategy(new DefaultRedirectStrategy {
				override def isRedirected(req: HttpRequest, res: HttpResponse, ctx: HttpContext) = false
			})
			client
		}
	}

	override def destroy {
		http.shutdown()
	}

	override def afterPropertiesSet {

	}
	
	def login(email:String, firstName:String, lastName:String): Option[Session] = {
		val session = new Session(this, null)
		session.userEmail = email
		session.userFirstName = firstName
		session.userLastName = lastName
		session.login() match {
			case Created(sessionId) if sessionId != "" => {
				val session = new Session(this, sessionId)
				session.userEmail = email
				session.userFirstName = firstName
				session.userLastName = lastName
				Some(session)
			}
			case _ => None
		}
	}

}

/**
 * Acquired from a call to Turnitin.login(), this will call Turnitin methods as a particular
 * user.
 */
class Session(turnitin: Turnitin, val sessionId: String) extends TurnitinMethods with Logging {
	
    import TurnitinDates._

	val excludeFromMd5 = Seq("dtend", "create_session", "session-id", "src", "apilang")

	var userEmail = "coursework@warwick.ac.uk"
	var userFirstName = "Coursework"
	var userLastName = "App"
	var userId = ""
		
	private val http = turnitin.http
	private def diagnostic = turnitin.diagnostic

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
    		functionId: String, // API function ID
    		pdata: Option[FileData], // optional file to put in "pdata" parameter
    		params: Pair[String, String]*) : TurnitinResponse = {

		val parameters = (Map("fid" -> functionId) ++ commonParameters ++ params).filter { case (key, value) => value != null }
		val postWithParams = turnitin.endpoint.POST << (parameters + md5hexparam(parameters))
		val req = addPdata(pdata, postWithParams)

		logger.debug("doRequest: " + parameters)

		val request: Handler[TurnitinResponse] =
			req >:+ { (headers, req) => 
				val location = headers("location").headOption
				if (location.isDefined) req >- { (text) => TurnitinResponse.redirect(location.get) }
				else if (turnitin.diagnostic) req >- { (text) => TurnitinResponse.fromDiagnostic(text) }
    			else req <> { (node) => TurnitinResponse.fromXml(node) }
			}
		http.x(request)
	}

	def doRequestAdvanced(functionId: String, // API function ID
		pdata: Option[FileData], // optional file to put in "pdata" parameter
		params: Pair[String, String]*) // POST parameters
		(transform: (Request) => (Handler[TurnitinResponse])): TurnitinResponse = {

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
	 */
	def addPdata(file: Option[FileData], req: Request) =
		file map (d =>
			req <<* ("pdata", d.name, { () => new FileInputStream(d.file) })) getOrElse req

	/**
	 * Parameters that we need in every request.
	 */
	def commonParameters = Map(
		"diagnostic" -> (if (diagnostic) "1" else "0"),
		"gmtime" -> gmtTimestamp,
		"encrypt" -> "0",
		"aid" -> turnitin.aid,
		"fcmd" -> "2",
		"uem" -> userEmail,
		"ufn" -> userFirstName,
		"uln" -> userLastName,
		"utp" -> "2",
		"src" -> turnitin.integrationId) ++ (subAccountParameter) ++ (sessionIdParameter)

	private def subAccountParameter: Map[String, String] =
		if (turnitin.said == null || turnitin.said.isEmpty) Map.empty
		else Map("said" -> turnitin.said)

	private def sessionIdParameter: Map[String, String] =
		if (sessionId == null) Map.empty
		else Map("session-id" -> sessionId)

	def md5hexparam(map: Map[String, String]) = ("md5" -> md5hex(map))

	/**
	 * Sort parameters by key, concatenate all the values with
	 * the shared key and MD5hex that.
	 */
	def md5hex(params: Map[String, String]) = {
		val sortedJoinedParams = params.filterKeys(!excludeFromMd5.contains(_)).toSeq
			.sortBy(_._1) // sort by key (left part of Pair)
			.map(_._2)    // map to value (right part of Pair)
			.mkString("")
		DigestUtils.md5Hex(sortedJoinedParams + turnitin.sharedSecretKey)
	}

	

}