package uk.ac.warwick.tabula.services.turnitin

import java.io.InputStream

import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.{HttpGet, HttpPost, HttpUriRequest}
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.helpers.Logging

import scala.reflect.runtime.universe._

case class FileData(data: InputStream, name: String)

object Turnitin {
	/**
		* ID that we should store classes under. They are per-module so we base it on the module code.
		* This ID is stored within Turnitin and requests for the same ID should return the same class.
		*/
	def classIdFor(assignment: Assignment, prefix: String) = ClassId(prefix + "-" + assignment.module.code)

	/**
		* ID that we should store assignments under. Our assignment ID is as good an identifier as any.
		* This ID is stored within Turnitin and requests for the same ID should return the same assignment.
		*/
	def assignmentIdFor(assignment: Assignment) = AssignmentId("Assignment-" + assignment.id)

	def classNameFor(assignment: Assignment): ClassName = {
		val module = assignment.module
		ClassName(module.code.toUpperCase + " - " + module.name)
	}

	def assignmentNameFor(assignment: Assignment): AssignmentName = {
		AssignmentName(assignment.name + " (" + assignment.academicYear.toString + ")")
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
class Turnitin extends Logging with DisposableBean {


	/** The top level account ID (usually for University of Warwick account) */
	@Value("${turnitin.aid}") var aid: String = _
	/** Sub-account ID underneath University of Warwick */
	@Value("${turnitin.said}") var said: String = _
	/** Shared key as set up on the University of Warwick account's Open API settings */
	@Value("${turnitin.key}") var sharedSecretKey: String = _

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
	def request[A <: HttpUriRequest : TypeTag]: A = {
		val request: A = typeOf[A] match {
			case t if t =:= typeOf[HttpGet] => new HttpGet(apiEndpoint).asInstanceOf[A]
			case t if t =:= typeOf[HttpPost] => new HttpPost(apiEndpoint).asInstanceOf[A]
			case t => throw new IllegalArgumentException(s"Unexpected type $t")
		}

		request.setHeader("User-Agent", userAgent)
		request
	}

	val httpClient: CloseableHttpClient =
		HttpClients.custom()
			.setDefaultRequestConfig(RequestConfig.custom()
				.setConnectTimeout(20000)
				.setSocketTimeout(20000)
				.build())
			.disableRedirectHandling()
			.disableCookieManagement()
			.build()

	override def destroy(): Unit = {
		httpClient.close()
	}

	def login(email:String, firstName:String, lastName:String): Option[Session] = {
		val session = new Session(this, null)
		val userEmail = if (email == null || email.isEmpty) firstName + lastName + "@turnitin.warwick.ac.uk" else email

		session.userEmail = userEmail
		session.userFirstName = firstName
		session.userLastName = lastName
		session.login() match {
			case Created(sessionId) if sessionId != "" =>
				val session = new Session(this, sessionId)
				session.userEmail = userEmail
				session.userFirstName = firstName
				session.userLastName = lastName
				session.acquireUserId()
				Some(session)
			case _ => None
		}
	}

}