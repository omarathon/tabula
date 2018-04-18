package uk.ac.warwick.tabula.helpers

import java.net.URI
import java.nio.charset.StandardCharsets

import org.apache.commons.codec.binary.Base64
import org.apache.http.auth.{AuthScope, Credentials}
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.client.utils.URIUtils
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.{BasicAuthCache, BasicCredentialsProvider}
import org.apache.http.impl.conn.DefaultSchemePortResolver
import org.apache.http.message.BasicHeader
import org.apache.http.{Header, HttpHost}

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
}

object ApacheHttpClientUtils extends ApacheHttpClientUtils
