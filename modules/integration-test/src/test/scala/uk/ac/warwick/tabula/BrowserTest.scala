package uk.ac.warwick.tabula

import org.scalatest._
import org.scalatest.junit._
import org.scalatest.selenium.WebBrowser
import org.junit.runner.RunWith
import com.thoughtworks.selenium.Selenium
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.WebDriverBackedSelenium
import org.openqa.selenium.WebDriver
import org.springframework.test.context.ContextConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Bean
import javax.annotation.Resource
import org.springframework.test.context.TestContextManager
import org.springframework.test.context.ContextConfiguration
import org.springframework.beans.factory.annotation.Autowired
import java.util.Properties
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.ie.InternetExplorerDriver

/**
 * Abstract base class for Selenium tests.
 * 
 * The WebBrowser trait does all the Selenium magic, hooking into
 * the WebDriver we provide.
 * 
 * Work in progress.
 */
@RunWith(classOf[JUnitRunner])
abstract class BrowserTest 
		extends ShouldMatchers
		with FlatSpec
		with WebBrowser {
	
	val P = FunctionalTestProperties
	
	def Path(path: String) = P.SiteRoot + path
	
	implicit lazy val webDriver: WebDriver = P.Browser match {
		case "htmlunit" => new HtmlUnitDriver(true) // JS enabled
		case "chrome" => new ChromeDriver
		case "firefox" => new FirefoxDriver
		case "ie" => new InternetExplorerDriver
	}
	
}

/**
 * Properties that can be overriden by a integrationtest.properties file in the classpath.
 * 
 * Defaults are in integrationtest-default.proerties.
 */
object FunctionalTestProperties {
	private val properties = loadOptionalProps("/integrationtest.properties", "/integrationtest-default.properties")

	val SiteRoot = prop("toplevel.url")
	val Browser = prop("browser")

	private def prop(name: String) = properties.getProperty(name)
	
	private def loadOptionalProps(filename: String, defaultsFilename: String) = {
		val properties = new Properties
		properties.load(getClass.getResourceAsStream(defaultsFilename))
		val input = getClass.getResourceAsStream(filename)
		if (input != null) properties.load(input)
		properties
	}

}



