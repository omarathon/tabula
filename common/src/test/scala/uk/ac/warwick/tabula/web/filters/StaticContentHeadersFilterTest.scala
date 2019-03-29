package uk.ac.warwick.tabula.web.filters

import java.util.Properties

import org.apache.http.{HttpHeaders, HttpStatus}
import org.springframework.mock.web.{MockFilterChain, MockHttpServletRequest, MockHttpServletResponse}
import uk.ac.warwick.tabula.TestBase

class StaticContentHeadersFilterTest extends TestBase {

  val filter = new StaticContentHeadersFilter
  filter.staticHashes = new Properties {{
    setProperty("css/styles.css", "150589499285")
  }}

  @Test
  def correctHash(): Unit = {
    val req = new MockHttpServletRequest
    val resp = new MockHttpServletResponse
    val chain = new MockFilterChain

    req.setRequestURI("/static/css/styles.150589499285.css")

    filter.doFilter(req, resp, chain)
    resp.getStatus should be (HttpStatus.SC_OK)
    resp.getHeader(HttpHeaders.CACHE_CONTROL) should be ("public, max-age=31536000, immutable")
  }

  @Test
  def incorrectHash(): Unit = {
    val req = new MockHttpServletRequest
    val resp = new MockHttpServletResponse
    val chain = new MockFilterChain

    req.setRequestURI("/static/css/styles.111111111111.css")

    filter.doFilter(req, resp, chain)
    resp.getStatus should be (HttpStatus.SC_MOVED_TEMPORARILY)
    resp.getHeader(HttpHeaders.LOCATION) should be ("/static/css/styles.150589499285.css")
  }

  @Test
  def hashNotFound(): Unit = {
    val req = new MockHttpServletRequest
    val resp = new MockHttpServletResponse
    val chain = new MockFilterChain

    req.setRequestURI("/static/css/wibble.1234567890123.css")

    filter.doFilter(req, resp, chain)

    // Just pass-through
    resp.getStatus should be (HttpStatus.SC_OK)
    resp.containsHeader(HttpHeaders.CACHE_CONTROL) should be (false)
  }

  @Test
  def noHashFound(): Unit = {
    val req = new MockHttpServletRequest
    val resp = new MockHttpServletResponse
    val chain = new MockFilterChain

    req.setRequestURI("/static/css/styles.css")

    filter.doFilter(req, resp, chain)
    resp.getStatus should be (HttpStatus.SC_MOVED_TEMPORARILY)
    resp.getHeader(HttpHeaders.LOCATION) should be ("/static/css/styles.150589499285.css")
  }

  @Test
  def noHashNotFound(): Unit = {
    val req = new MockHttpServletRequest
    val resp = new MockHttpServletResponse
    val chain = new MockFilterChain

    req.setRequestURI("/static/css/wibble.css")

    filter.doFilter(req, resp, chain)

    // Just pass-through
    resp.getStatus should be (HttpStatus.SC_OK)
    resp.containsHeader(HttpHeaders.CACHE_CONTROL) should be (false)
  }

}
