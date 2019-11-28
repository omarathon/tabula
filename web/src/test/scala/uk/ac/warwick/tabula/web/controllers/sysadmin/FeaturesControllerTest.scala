package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.scalatestplus.mockito.MockitoSugar
import uk.ac.warwick.tabula.{Features, FeaturesMessage, TestBase}
import uk.ac.warwick.util.queue.Queue
import uk.ac.warwick.util.queue.conversion.AnnotationJsonObjectConverter

class FeaturesControllerTest extends TestBase with MockitoSugar {
  @Test def access(): Unit = {
    val features = Features.empty
    val controller = new FeaturesController
    controller.features = features
    controller.queue = mock[Queue]

    controller.afterPropertiesSet()

    controller.currentValues should not be 'empty

    controller.currentValues.find(_.name == "emailStudents").get.value should be(false)

    features.emailStudents = true

    controller.currentValues.find(_.name == "emailStudents").get.value should be(true)

    withUser("cuscav") {
      controller.update("emailStudents", value = false)
    }

    controller.currentValues.find(_.name == "emailStudents").get.value should be(false)
  }

  @Test def featuresMessageConversion(): Unit = {
    val json = new AnnotationJsonObjectConverter
    val features = Features.empty
    features.emailStudents = true
    val featuresMessage = new FeaturesMessage(features)
    val string = json.toJson(featuresMessage).toString()
    string should not be "{}"
  }
}
