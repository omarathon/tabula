package uk.ac.warwick.tabula.helpers

import java.io.InputStreamReader
import java.net.URI
import java.nio.charset.StandardCharsets

import dispatch.classic.Handler
import javax.xml.parsers.SAXParserFactory
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import org.apache.http.auth.{AuthScope, Credentials}
import org.apache.http.client.ResponseHandler
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.client.utils.URIUtils
import org.apache.http.entity.ContentType
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.{AbstractResponseHandler, BasicAuthCache, BasicCredentialsProvider}
import org.apache.http.impl.conn.DefaultSchemePortResolver
import org.apache.http.message.BasicHeader
import org.apache.http.util.EntityUtils
import org.apache.http.{Header, HttpEntity, HttpHost}

import scala.xml.XML

trait ApacheHttpClientUtils {
	def preemptiveBasicAuthContext(uri: URI, credentials: Credentials): HttpClientContext = {
		val host = new HttpHost(uri.getHost, DefaultSchemePortResolver.INSTANCE.resolve(URIUtils.extractHost(uri)), uri.getScheme)

		val credsProvider = new BasicCredentialsProvider
		credsProvider.setCredentials(new AuthScope(host), credentials)

		// Create AuthCache instance
		val authCache = new BasicAuthCache
		// Generate BASIC scheme object and add it to the local auth cache
		val basicAuth = new BasicScheme
		authCache.put(host, basicAuth)

		// Add AuthCache to the execution context
		val context = HttpClientContext.create
		context.setCredentialsProvider(credsProvider)
		context.setAuthCache(authCache)
		context
	}

	def basicAuthHeader(credentials: Credentials): Header = {
		val combinedCredentials = s"${credentials.getUserPrincipal.getName}:${credentials.getPassword}"
		val encodedCredentials = Base64.encodeBase64String(combinedCredentials.getBytes(StandardCharsets.UTF_8))

		new BasicHeader("Authorization", s"Basic $encodedCredentials")
	}

	def xmlResponseHandler[A](block: xml.Elem => A): ResponseHandler[A] =
		new AbstractResponseHandler[A] {
			override def handleEntity(entity: HttpEntity): A = {
				val in = entity.getContent
				try {
					val charset = ContentType.getLenientOrDefault(entity).getCharset
					val reader = new InputStreamReader(in, charset)
					val xml = XML.withSAXParser(Handler.saxParserFactory.newSAXParser).load(reader)
					block(xml)
				} finally {
					IOUtils.closeQuietly(in)
					EntityUtils.consumeQuietly(entity)
				}
			}
		}
}

object ApacheHttpClientUtils extends ApacheHttpClientUtils {
	// retain factory to use with XML.load; its newInstance method is not thread-safe
	lazy val saxParserFactory: SAXParserFactory = {
		val spf = SAXParserFactory.newInstance()
		spf.setNamespaceAware(false)
		spf
	}
}