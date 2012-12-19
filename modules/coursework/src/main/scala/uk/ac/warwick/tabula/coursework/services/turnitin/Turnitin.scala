package uk.ac.warwick.tabula.coursework.services.turnitin

import java.io.File
import org.apache.commons.io.FilenameUtils.getExtension
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.impl.client.DefaultRedirectStrategy
import org.apache.http.protocol.HttpContext
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import dispatch._
import dispatch.Request.toRequestVerbs
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.CurrentUser
import org.apache.http.cookie.CookieSpec
import org.apache.commons.httpclient.cookie.IgnoreCookiesSpec
import org.apache.http.cookie.CookieSpecRegistry
import org.apache.http.client.params.ClientPNames
import org.apache.http.client.params.CookiePolicy
import uk.ac.warwick.tabula.data.model.Assignment
import scala.util.matching.Regex
import dispatch.thread.ThreadSafeHttpClient

case class FileData(val file: File, val name: String)

object Turnitin {
	/**
	 * Quoted supported types are...
	 * "MS Word, Acrobat PDF, Postscript, Text, HTML, WordPerfect (WPD) and Rich Text Format".
	 */
	val validExtensions = Seq("doc", "docx", "pdf", "rtf", "txt", "wpd", "htm", "html", "ps")

	def validFileType(file: FileAttachment): Boolean =
		Turnitin.validExtensions contains getExtension(file.name).toLowerCase
		
	/**
     * ID that we should store classes under. They are per-module so we base it on the module code.
     * This ID is stored within Turnitin and requests for the same ID should return the same class.
     */
    def classIdFor(assignment: Assignment, prefix: String) = ClassId(prefix+"-" + assignment.module.code)
    
    /**
     * ID that we should store assignments under. Our assignment ID is as good an identifier as any.
     * This ID is stored within Turnitin and requests for the same ID should return the same assignment.
     */
    def assignmentIdFor(assignment: Assignment) = AssignmentId("Assignment-" + assignment.id)
    
    def classNameFor(assignment: Assignment) = {
        val module = assignment.module
        ClassName(module.code.toUpperCase() + " - " + module.name)
    }
    
    def assignmentNameFor(assignment: Assignment) = {
        AssignmentName(assignment.name)
    } 
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
	
	@Value("${turnitin.class.prefix}") var classPrefix: String =_

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
		override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), maxConnections, maxConnectionsPerRoute) {
			setRedirectStrategy(new DefaultRedirectStrategy {
				override def isRedirected(req: HttpRequest, res: HttpResponse, ctx: HttpContext) = false
			})
			getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
		}	
	}

	override def destroy {
		http.shutdown()
	}

	override def afterPropertiesSet {

	}
	
	def login(user: CurrentUser): Option[Session] = login( user.email, user.firstName, user.lastName )
	
	def login(email:String, firstName:String, lastName:String): Option[Session] = {
		val session = new Session(this, null)
		val userEmail = if (email == null || email.isEmpty()) firstName + lastName + "@turnitin.warwick.ac.uk" else email
			
		session.userEmail = userEmail
		session.userFirstName = firstName
		session.userLastName = lastName
		session.login() match {
			case Created(sessionId) if sessionId != "" => {
				val session = new Session(this, sessionId)
				session.userEmail = userEmail
				session.userFirstName = firstName
				session.userLastName = lastName
				session.acquireUserId()
				Some(session)
			}
			case _ => None
		}
	}

}

