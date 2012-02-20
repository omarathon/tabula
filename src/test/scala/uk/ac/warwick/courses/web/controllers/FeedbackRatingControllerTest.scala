package uk.ac.warwick.courses.web.controllers
import uk.ac.warwick.courses.TestBase
import org.junit.Test
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter
import uk.ac.warwick.courses.AppContextTestBase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.HandlerAdapter

class FeedbackRatingControllerTest extends AppContextTestBase {
  
  @Autowired var controller:FeedbackRatingController =_
  @Autowired var handler:HandlerAdapter =_
  
  @Test def invoke {
    val req = testRequest(uri="/module/12345/12346/rate")
    val res = testResponse
    
    handler.handle(req, res, controller)
  }
}