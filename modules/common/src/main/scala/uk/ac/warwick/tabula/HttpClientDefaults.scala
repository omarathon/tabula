package uk.ac.warwick.tabula

import org.springframework.beans.factory.annotation.Value

object HttpClientDefaults {

	@Value("${httpclient.connectTimeout}") var connectTimeout: Int = _

	@Value("${httpclient.socketTimeout}") var socketTimeout: Int = _

}
