package uk.ac.warwick.tabula.web

import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}

trait SimpleHttpFetching {
	lazy val httpClient: CloseableHttpClient = SimpleHttpFetching.httpClient
}

object SimpleHttpFetching {
	lazy val httpClient: CloseableHttpClient =
		HttpClients.custom()
  		.disableCookieManagement()
  		.build()
}