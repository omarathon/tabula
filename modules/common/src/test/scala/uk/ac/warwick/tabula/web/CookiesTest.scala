package uk.ac.warwick.tabula.web

import uk.ac.warwick.tabula.TestBase
import javax.servlet.http

class CookiesTest extends TestBase {

	@Test
	def getValues {
		val array = Array[http.Cookie](
			new http.Cookie("name", "Billy"),
			new http.Cookie("isGood", "true"),
			new http.Cookie("isNasty", "false"),
			new http.Cookie("isNonsense", "blahblah")
		)

		val cookies = new Cookies(array)
		cookies.getString("name") should be (Some("Billy"))
		cookies.getString("wasd") should be (None)
		cookies.getBoolean("isGood", false) should be (true)
		cookies.getBoolean("isNasty", false) should be (false)
		cookies.getBoolean("isNonsense", false) should be (false)
	}

	/* Request can have a null array of cookies if the user agent sends no header */
	@Test
	def nullArray {
		val cookies = new Cookies(null)
		cookies.getString("name") should be (None)
		cookies.getCookie("name") should be (None)
		cookies.getBoolean("a", false) should be (false)
		cookies.getBoolean("a", true) should be (true)
	}

}