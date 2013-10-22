package uk.ac.warwick.tabula.home

import uk.ac.warwick.tabula.BrowserTest
import org.openqa.selenium.By

trait FeaturesDriver extends BrowserTest{

	def setFeatureState(name:String, state:Boolean){
		signIn as(P.Sysadmin) to (Path("/sysadmin/features"))
		val featureForm = webDriver.findElement(By.id(name + "_form"))
		val buttonText = state.toString
		val button = featureForm.findElement(By.xpath(s"./input[@type='submit' and @value='$buttonText']"))
		button.click()
		// wait a second here in the hope that all apps have received the message.
		// even better would be to register our own JMS consumer and wait until *we* see the message
		Thread.sleep(1000)
	}
	def enableFeature(name:String){
		setFeatureState(name, true)
	}

	def disableFeature(name:String){
		setFeatureState(name, false)
	}

}
