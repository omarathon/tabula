package uk.ac.warwick.tabula

import org.scalatest._
import org.scalatest.junit._
import org.scalatest.selenium.WebBrowser
import org.junit.runner.RunWith
import com.thoughtworks.selenium.Selenium
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.WebDriverBackedSelenium
import org.openqa.selenium.WebDriver
import java.util.Properties
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.ie.InternetExplorerDriver
import java.io.File
import java.io.FileInputStream
import org.specs.matcher.EventuallyMatchers
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar
import org.openqa.selenium.internal.seleniumemulation.WaitForPageToLoad
import com.gargoylesoftware.htmlunit.BrowserVersion

/** Abstract base class for Selenium tests.
  *
  * The WebBrowser trait does all the Selenium magic, hooking into
  * the WebDriver we provide.
  */
@RunWith(classOf[JUnitRunner])
abstract class BrowserTest
	extends ShouldMatchers
	with FlatSpec
	with BeforeAndAfter
	with Eventually
	with SpanSugar
	with WebBrowser
	with WebsignonMethods {

	// Shorthand to expose properties to test classes
	val P = FunctionalTestProperties

	/** Generates a full URL to browse to, 
	  * e.g. Path("/coursework") -> "https://tabula-test.warwick.ac.uk/coursework"
	  */
	def Path(path: String) = P.SiteRoot + path

	implicit lazy val webDriver: WebDriver = P.Browser match {
		case "htmlunit" => { new HtmlUnitDriver(true) // JS enabled
//			val driver = new HtmlUnitDriver(BrowserVersion.INTERNET_EXPLORER_8) // JS enabled
//			driver.setJavascriptEnabled(true)
//			driver
		}
		case "chrome" => new ChromeDriver
		case "firefox" => new FirefoxDriver
		case "ie" => new InternetExplorerDriver
	}

	def ifHtmlUnitDriver(operation:HtmlUnitDriver=>Unit) = {
		webDriver match {
			case h:HtmlUnitDriver=>operation(h)
			case _=> // do nothing
		}
	}
	
	/**
	 * eventually{} is a generic ScalaTest method to repeatedly
	 * try a block of code until it works or we give up. eventuallyAjax {}
	 * just calls that with some sensible default timeouts.
	 */
	def eventuallyAjax(fun: =>Unit) {
		eventually(timeout(10.seconds), interval(200.millis)) (fun)
	}

}

case class LoginDetails(val usercode: String, val password: String, description: String)

/** Properties that can be overridden by a functionaltest.properties file in the classpath.
  *
  * Can also be overridden by a System property, which can be useful e.g. for running a similar
  * set of tests multiple times with a different browser. (Note that some properties cannot be
  * system properties, such as 
  *
  * Defaults are in functionaltest-default.properties.
  */
object FunctionalTestProperties {
	private val properties = loadOptionalProps()

	val SiteRoot = prop("toplevel.url")
	val Browser = prop("browser")

	/* Test user accounts who can sign in during tests. Populated from properties.
	 * The tests currently REQUIRE that the user's first name is
	 * equal to the usercode, since we look for "Signed in as X" to
	 * determine whether we're signed in. Open to a better solution. 
	 */
	lazy val Admin1 = userDetails("admin1", "Departmental admin")
	lazy val Admin2 = userDetails("admin2", "Departmental admin")
	lazy val ExtensionManager1 = userDetails("extman1", "Extension manager")
	lazy val ExtensionManager2 = userDetails("extman2", "Extension manager")
	lazy val Marker1 = userDetails("marker1", "Marker")
	lazy val Marker2 = userDetails("marker2", "Marker")
	lazy val Marker3 = userDetails("marker3", "Marker")
	lazy val ModuleManager1 = userDetails("modman1", "Module Manager")
	lazy val ModuleManager2 = userDetails("modman2", "Module Manager")
	lazy val Student1 = userDetails("student1", "Student")
	lazy val Student2 = userDetails("student2", "Student")
	lazy val Student3 = userDetails("student3", "Student")
	lazy val Student4 = userDetails("student4", "Student")
	lazy val Student5 = userDetails("student5", "Student")
  lazy val Sysadmin = userDetails("sysadmin", "System Administrator")
	/**
	 * Get a property by name, or null if not found anywhere. Checks in this order
	 * - System properties
	 * - provided tabula-functionaltest.properties
	 * - default properties 
	 */
	private def prop(name: String) = 
		scala.util.Properties.propOrElse(name, fileProp(name))
		
	/** Like prop() but excludes system properties */
	private def fileProp(name: String) = properties.getProperty(name)

	private def loadOptionalProps() = {
		val file = new File(System.getProperty("user.home"), "tabula-functionaltest.properties")
		if (!file.exists()) {
			Assertions.fail("Properties file missing:  " + file)
		}
		val properties = new Properties
		properties.load(getClass.getResourceAsStream("/functionaltest-default.properties"))
		properties.load(new FileInputStream(file))
		properties
	}

	private def userDetails(identifier: String, description: String) = {
		val usercodeKey = "user." + identifier + ".usercode"
		val passwordKey = "user." + identifier + ".password"
		if (properties.containsKey(usercodeKey)) {
			LoginDetails(
				fileProp("user." + identifier + ".usercode"),
				fileProp("user." + identifier + ".password"),
				description)
		} else {
			Assertions.fail("Properties missing for "+description+" (user."+identifier+".usercode)")
		}
	}
}
