package uk.ac.warwick.tabula.home

import uk.ac.warwick.tabula.BrowserTest
import org.openqa.selenium.By
import dispatch.classic.thread.ThreadSafeHttpClient
import dispatch.classic.NoLogging
import dispatch.classic._
import org.apache.http.client.params.{CookiePolicy, ClientPNames}
import dispatch.classic.thread.ThreadSafeHttpClient
import uk.ac.warwick.tabula.FunctionalTestProperties
import org.apache.http.HttpStatus

trait FeaturesDriver extends BrowserTest with SimpleHttpFetching {

	def setFeatureState(name:String, state:Boolean){
		signIn as(P.Sysadmin) to (Path("/sysadmin/features"))
		val featureForm = webDriver.findElement(By.id(name + "_form"))
		val buttonText = state.toString
		val button = featureForm.findElement(By.xpath(s"./input[@type='submit' and @value='$buttonText']"))
		button.click()
		
		// wait a second here in the hope that all apps have received the message.
		// even better would be to register our own JMS consumer and wait until *we* see the message
		eventually {
			isFeatureEnabled(name) should be (state)
		}
	}
	
	def enableFeature(name:String) {
		setFeatureState(name, true)
	}

	def disableFeature(name:String) {
		setFeatureState(name, false)
	}
	
	def isFeatureEnabled(name: String) = {
		Seq("", "/admin", "/attendance", "/coursework", "/groups", "/profiles", "/scheduling").forall { context =>
			val uri = FunctionalTestProperties.SiteRoot + context + "/test/feature/" + name
			val req = url(uri)
			
			val resp = http.when(_ == HttpStatus.SC_OK) { req.as_str }
			resp.toBoolean
		}
	}

}
