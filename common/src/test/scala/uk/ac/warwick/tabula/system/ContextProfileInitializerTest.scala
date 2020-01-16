package uk.ac.warwick.tabula.system

import org.springframework.mock.env.MockPropertySource
import uk.ac.warwick.tabula.TestBase

class ContextProfileInitializerTest extends TestBase {
  @Test def devWeb(): Unit = {
    val initializer = new ContextProfileInitializer
    initializer.testConfig = mockProperties(
      "spring.profiles.active" -> "dev",
      "scheduling.enabled" -> "true"
    )
    val profiles = initializer.resolve()
    profiles should (
      have size 3 and
        contain("dev") and
        contain("web") and
        contain("scheduling")
      )
  }

  @Test def prodNoWeb(): Unit = {
    val initializer = new ContextProfileInitializer
    initializer.testConfig = mockProperties(
      "spring.profiles.active" -> "production",
      "web.enabled" -> "false",
      "api.enabled" -> "false"
    )
    val profiles = initializer.resolve()
    profiles should (
      have size 1 and
        contain("production")
      )
  }

  @Test def defaults(): Unit = {
    val initializer = new ContextProfileInitializer
    val profiles = initializer.resolve()
    profiles should (
      have size 2 and
        contain("test") and
        contain("web")
      )
  }

  private def mockProperties(pairs: (String, String)*) =
    new MockPropertySource {
      for ((key, value) <- pairs) {
        setProperty(key, value)
      }
    }

}