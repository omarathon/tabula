package uk.ac.warwick.tabula.system

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import org.springframework.web.context.request.WebRequestInterceptor
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.ServletWebRequest

class ConditionalInterceptorTest extends TestBase with Mockito {
	
	val delegate = mock[WebRequestInterceptor]
	val interceptor = new ConditionalInterceptor(delegate)
	interceptor.excludePath = "/sysadmin/*"
	interceptor.afterPropertiesSet()
		
	val request = new MockHttpServletRequest
	val servletRequest = new ServletWebRequest(request)
	
	@Test def nonMatching {		
		interceptor.preHandle(servletRequest)
		there was one(delegate).preHandle(servletRequest)
		
		interceptor.postHandle(servletRequest, null)
		there was one(delegate).postHandle(servletRequest, null)
		
		interceptor.afterCompletion(servletRequest, null)
		there was one(delegate).afterCompletion(servletRequest, null)
	}
	
	@Test def matching {
		request.setContextPath("/tabula")
		request.setRequestURI("/tabula/sysadmin/home")
		
		interceptor.preHandle(servletRequest)
		there was no(delegate).preHandle(servletRequest)
		
		interceptor.postHandle(servletRequest, null)
		there was no(delegate).postHandle(servletRequest, null)
		
		interceptor.afterCompletion(servletRequest, null)
		there was no(delegate).afterCompletion(servletRequest, null)
	}

}