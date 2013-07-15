package uk.ac.warwick.tabula.home

import uk.ac.warwick.tabula.BrowserTest
import org.openqa.selenium.By

class FeaturesDriver extends BrowserTest{

	def setFeatureState(name:String, state:Boolean){
		signIn as(P.Sysadmin) to (Path("/sysadmin/features"))
		val featureForm = webDriver.findElement(By.id(name + "_form"))
		val buttonText = state.toString
		val button = featureForm.findElement(By.xpath(s"./input[@type='submit' and @value='$buttonText']"))
		button.click()
		// possibly we should wait a while here to ensure that all apps have received the message.
		// even better would be to register our own JMS consumer and wait until *we* see the message
	}
}
object FeaturesDriver{
	val driver = new FeaturesDriver

	def enableFeature(name:String){
		driver.setFeatureState(name, true)
	}

	def disableFeature(name:String){
		driver.setFeatureState(name, false)
	}

}
