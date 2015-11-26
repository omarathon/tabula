package uk.ac.warwick.tabula

import uk.ac.warwick.spring.Wire

object HttpClientDefaults {

	var connectTimeout: Int = Integer.parseInt(Wire.optionProperty("${httpclient.connectTimeout}").getOrElse("5000"))

	var socketTimeout: Int = Integer.parseInt(Wire.optionProperty("${httpclient.socketTimeout}").getOrElse("5000"))

}