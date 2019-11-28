package uk.ac.warwick.tabula.web

import javax.servlet.http
import javax.servlet.http.HttpServletResponse

import scala.language.implicitConversions

/**
  * A Scala-ish wrapper for servlet Cookie.
  */
class Cookie(val cookie: http.Cookie) {
  def this(
    name: String,
    value: String,
    maxAge: Option[Int] = None,
    path: String = "/",
    domain: Option[String] = None,
    secure: Boolean = true,
    httpOnly: Boolean = true
  ) = {
    this(new http.Cookie(name, value))
    this.maxAge = maxAge
    this.path = path
    domain.foreach(this.domain = _)
    this.secure = secure
    this.httpOnly = httpOnly
  }

  def value: String = cookie.getValue
  def value_=(value: String): Unit = cookie.setValue(value)

  def maxAge: Option[Int] = Option(cookie.getMaxAge).filterNot(_ == -1)
  def maxAge_=(expiry: Option[Int]): Unit = cookie.setMaxAge(expiry.getOrElse(-1))

  def path: String = cookie.getPath
  def path_=(p: String): Unit = cookie.setPath(p)

  def domain: String = cookie.getDomain
  def domain_=(d: String): Unit = cookie.setDomain(d)

  def secure: Boolean = cookie.getSecure
  def secure_=(f: Boolean): Unit = cookie.setSecure(f)

  def httpOnly: Boolean = cookie.isHttpOnly
  def httpOnly_=(f: Boolean): Unit = cookie.setHttpOnly(f)
}

/**
  * Scala-ish wrapper for the array of servlet Cookies from a request.
  */
class Cookies(val _cookies: Array[http.Cookie]) {
  lazy val cookies: Array[http.Cookie] = if (_cookies == null) Array.empty[http.Cookie] else _cookies

  def getCookie(name: String): Option[Cookie] = wrap(cookies.find(_.getName == name))

  def getString(name: String): Option[String] = getCookie(name) match {
    case Some(cookie) => Some(cookie.value)
    case None => None
  }

  def getBoolean(name: String, default: Boolean): Boolean = getCookie(name) match {
    case Some(cookie) => try {
      cookie.value.toBoolean
    } catch {
      case _@(_: NumberFormatException | _: IllegalArgumentException) => default
    }
    case None => default
  }

  private def wrap(cookie: Option[http.Cookie]): Option[Cookie] = cookie match {
    case Some(cookie) => Some(new Cookie(cookie))
    case None => None
  }
}

/**
  * Defines implicit conversions from boring cookie arrays to nice useful Cookies objects,
  * and adds implicit methods to HttpServletResponse to support adding our Cookie class.
  */
object Cookies {
  implicit def toMagicCookies(cookies: Array[http.Cookie]): Cookies = new Cookies(cookies)

  implicit class CookieMethods(response: HttpServletResponse) {
    def addCookie(cookie: Cookie): Unit = response.addCookie(cookie.cookie)
  }
}
