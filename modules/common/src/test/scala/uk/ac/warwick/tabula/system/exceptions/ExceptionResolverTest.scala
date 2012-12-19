package uk.ac.warwick.tabula.system.exceptions

import scala.collection.JavaConversions._

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.PermissionDeniedException
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.actions._
import uk.ac.warwick.tabula.ItemNotFoundException

class ExceptionResolverTest extends TestBase {
	
	val resolver = new ExceptionResolver {			
		defaultView = "defaultErrorView"
		exceptionHandler = new ExceptionHandler {
			override def exception(context: ExceptionContext) {
				// no special handling.
			}
		}
		
		viewMappings = Map(
			classOf[ItemNotFoundException].getName -> "could-not-find"
		)
		
		override def loginUrl = "https://websignon.example.com/?target=whatever&etc"
	}
	
	/**
	 * When a PermissionDeniedException is thrown and the user is not
	 * signed in, we should always redirect to sign in rather than present
	 * a permission denied page.
	 */
	@Test
	def permDeniedNotLoggedIn {
		withUser(null) {
			val requestInfo = RequestInfo.fromThread.get
			val user = requestInfo.user
			val exception = new PermissionDeniedException(user, View(null), null)
			val modelAndView = resolver.doResolve(exception, None)
			modelAndView.viewName should be ("redirect:"+resolver.loginUrl)
		}
	}
	
	/**
	 * A PermissionDeniedException when logged in should be handled normally.
	 * Usually we'd have an explicit mapping from PermissionDeniedException to
	 * a special view but in this test it'll fall back to the default error view.
	 */
	@Test
	def permDeniedLoggedIn {
		withUser("cusebr") {
			val requestInfo = RequestInfo.fromThread.get
			val user = requestInfo.user
			val exception = new PermissionDeniedException(user, View(null), null)
			val modelAndView = resolver.doResolve(exception, None)
			modelAndView.viewName should be (resolver.defaultView)
		}
	}
	
	/**
	 * When an exception mapping exists for an exception, use the
	 * view name it maps to rather than defaultView.
	 */
	@Test
	def viewMapping {
		withUser("cusebr") {
			val modelAndView = resolver.doResolve(new ItemNotFoundException(), None)
			modelAndView.viewName should be ("could-not-find")
		}
	}

}