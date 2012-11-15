package uk.ac.warwick.tabula.coursework.web.controllers
import uk.ac.warwick.tabula.TestBase
import org.junit.Test
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter
import uk.ac.warwick.tabula.AppContextTestBase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.HandlerAdapter
import javax.annotation.Resource
import org.springframework.web.servlet.HandlerMapping
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model.Feedback


class FeedbackRatingControllerTest extends TestBase with Mockito {
  
//  @Autowired var controller:FeedbackRatingController =_
//  @Resource(name="requestMappingHandlerAdapter") var handler:HandlerAdapter =_
//  @Autowired var mapping:HandlerMapping =_
  
  @Test def invoke {
//    val req = testRequest(uri="/module/12345/12346/rate")
//    val res = testResponse
//    
//    val m = mapping.getHandler(req)
//    
//    handler.handle(req, res, m)
	  withUser("cusebr") {
		  val module = new Module
		  val assignment = new Assignment
		  assignment.module = module
		  val feedback = new Feedback
		  feedback.assignment = assignment
		  
		  val controller = new FeedbackRatingController
		  controller.features = emptyFeatures
		  controller.feedbackDao = mock[FeedbackDao]
		  controller.feedbackDao.getFeedbackByUniId(any[Assignment], any[String]) returns Some(feedback)
		   
//		  val cmd = controller.cmd(assignment, module)
//		  val mav = controller.form(cmd)
	  }
  }
}