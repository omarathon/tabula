package uk.ac.warwick.tabula.services

import dispatch.classic.thread.ThreadSafeHttpClient
import dispatch.classic.{Http, thread}
import org.apache.http.client.HttpClient
import org.apache.http.client.params.{ClientPNames, CookiePolicy}
import org.apache.http.params.HttpConnectionParams
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ScalaFactoryBean

@Service
class DispatchHttpClientFactoryBean extends ScalaFactoryBean[Http] {

	@Value("${httpclient.connectTimeout:20000}") var connectTimeout: Int = _
	@Value("${httpclient.socketTimeout:20000}") var socketTimeout: Int = _
	@Value("${httpclient.maxConnections:250}") var maxConnections: Int = _
	@Value("${httpclient.maxConnectionsPerRoute:100}") var maxConnectionsPerRoute: Int = _

	override def createInstance(): Http = new Http with thread.Safety {
		override def make_client: HttpClient = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), DispatchHttpClientFactoryBean.this.maxConnections, DispatchHttpClientFactoryBean.this.maxConnectionsPerRoute) {
			HttpConnectionParams.setConnectionTimeout(getParams, DispatchHttpClientFactoryBean.this.connectTimeout)
			HttpConnectionParams.setSoTimeout(getParams, DispatchHttpClientFactoryBean.this.socketTimeout)
			getParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
		}
	}

	override def destroyInstance(http: Http): Unit = http.shutdown()

}

trait DispatchHttpClientComponent {
	def httpClient: Http
}

trait AutowiringDispatchHttpClientComponent extends DispatchHttpClientComponent {
	var httpClient: Http = Wire[Http]
}