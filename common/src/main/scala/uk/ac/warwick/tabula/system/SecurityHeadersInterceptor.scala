package uk.ac.warwick.tabula.system

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter

class SecurityHeadersInterceptor extends HandlerInterceptorAdapter {
  override def preHandle(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any
  ): Boolean = {
    response.setHeader("X-Frame-Options", "SAMEORIGIN")
    response.setHeader("X-XSS-Protection", "1; mode=block")
    response.setHeader("X-Content-Type-Options", "nosniff")
    response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin")
    response.setHeader("Feature-Policy",
      Seq(
        "accelerometer 'self' https://my.warwick.ac.uk",
        "camera 'none'",
        "geolocation 'none'",
        "gyroscope 'self' https://my.warwick.ac.uk",
        "magnetometer 'none'",
        "microphone 'none'",
        "payment 'none'",
        "usb 'none'"
      ).mkString("; ")
    )

    true // allow request to continue
  }
}
