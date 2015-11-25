package uk.ac.warwick.tabula

import uk.ac.warwick.spring.Wire

object HttpClientDefaults {

	var connectTimeout: Int = Integer.parseInt(Wire.property("${httpclient.connectTimeout}"))

	var socketTimeout: Int = Integer.parseInt(Wire.property("${httpclient.socketTimeout}"))

}
