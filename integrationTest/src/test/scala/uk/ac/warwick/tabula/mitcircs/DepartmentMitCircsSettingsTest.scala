package uk.ac.warwick.tabula.mitcircs

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest

class DepartmentMitCircsSettingsTest extends BrowserTest with GivenWhenThen with MitCircsFixture {

  "Department admin" should "be able to enable mit circs for a department" in as(P.Admin2) {
    openDepartmentSettings()

    // Default settings
    Then("I should be able to see mit circs is not currently enabled")
    checkbox("enableMitCircs").isSelected should be (false)
    textArea("mitCircsGuidance").value should be (empty)

    // Enable settings
    When("I enable mit circs and provide guidance")
    click on checkbox("enableMitCircs")
    textArea("mitCircsGuidance").value =
      """Please submit your mit circs claims directly to the Night Heron.
        |
        |- Spit at the herons
        |- Maybe punch one
        |
        |There is no need to be upset.""".stripMargin

    Then("I should see a preview of the HTML version of the markdown guidance")
    eventually {
      id("mitCircsGuidance-preview").webElement.isDisplayed should be (true)
    }

    pageSource should include ("<li>Spit at the herons</li>")

    When("I click Save")
    submit()

    Then("I should be redirected back to the admin page")
    currentUrl should endWith ("/department/xxx")

    // Open the page again to make sure the settings have persisted
    openDepartmentSettings()

    // Default settings
    Then("I should be able to see mit circs is now enabled")
    checkbox("enableMitCircs").isSelected should be (true)
    textArea("mitCircsGuidance").value should include ("- Spit at the herons")
  }

  "User access manager" should "be able to set the MCO" in {
    enableMitCircsAndSetUpMCO()

    // This signs in to the homepage so we don't hit TAB-8007
    as(P.ExtensionManager1) {
      linkText("Mitigating Circumstances").webElement should not be null
    }
  }

}
