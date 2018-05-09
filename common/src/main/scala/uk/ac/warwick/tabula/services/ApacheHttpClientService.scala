package uk.ac.warwick.tabula.services

import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ScalaFactoryBean

@Service
class ApacheHttpClientFactoryBean extends ScalaFactoryBean[CloseableHttpClient] {
	@Value("${httpclient.connectTimeout:20000}") var connectTimeout: Int = _
	@Value("${httpclient.socketTimeout:20000}") var socketTimeout: Int = _
	@Value("${httpclient.maxConnections:250}") var maxConnections: Int = _
	@Value("${httpclient.maxConnectionsPerRoute:100}") var maxConnectionsPerRoute: Int = _

	override def createInstance(): CloseableHttpClient = HttpClients.custom()
		.setConnectionManager({
			val manager = new PoolingHttpClientConnectionManager()
			manager.setMaxTotal(maxConnections)
			manager.setDefaultMaxPerRoute(maxConnectionsPerRoute)
			manager
		})
		.setDefaultRequestConfig(RequestConfig.custom()
			.setConnectTimeout(connectTimeout)
			.setSocketTimeout(socketTimeout)
			.build())
		.disableCookieManagement()
		.build()

	override def destroyInstance(instance: CloseableHttpClient): Unit = instance.close()
}

trait ApacheHttpClientComponent {
	def httpClient: CloseableHttpClient
}

trait AutowiringApacheHttpClientComponent extends ApacheHttpClientComponent {
	var httpClient: CloseableHttpClient = Wire[CloseableHttpClient]
}