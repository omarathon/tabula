package uk.ac.warwick.tabula

import java.io.Reader

import org.apache.http.HttpStatus
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.tabula.helpers.ApacheHttpClientUtils

class FileDownload(uri: String, credentials: Option[LoginDetails]) {

	def request: RequestBuilder = RequestBuilder.get(uri)
	val authorizedRequest: RequestBuilder = credentials match {
		case Some(cred) => request.setHeader(ApacheHttpClientUtils.basicAuthHeader(new UsernamePasswordCredentials(cred.usercode, cred.password)))
		case _ => request
	}

	def streamToString(is:Reader):String = FileCopyUtils.copyToString(is)

	case class Result(content:Option[String], statusCode:Option[Int])

	lazy val result: Result =
		Download.httpClient.execute(authorizedRequest.build(), ApacheHttpClientUtils.handler {
			case response if response.getStatusLine.getStatusCode < 300 =>
				// treat things like 201, 204, as 200.
				Result(Some(EntityUtils.toString(response.getEntity)), Some(HttpStatus.SC_OK))

			case response =>
				EntityUtils.consumeQuietly(response.getEntity)
				Result(None, Some(response.getStatusLine.getStatusCode))
		})

	def isSuccessful: Boolean = result.statusCode.contains(HttpStatus.SC_OK)
	def contentAsString: String = result.content.getOrElse("")
	def as(credentials:LoginDetails) = new FileDownload(uri, Some(credentials))
}

object Download {
	lazy val httpClient: HttpClient =
		HttpClients.custom()
			.disableCookieManagement()
			.build()

	def apply(url: String): FileDownload = {
		new FileDownload(url, None)
	}
}