package uk.ac.warwick.tabula.web.filters

import javax.servlet.FilterChain
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import uk.ac.warwick.sso.client.SSOClientFilter
import uk.ac.warwick.sso.client.tags.SSOLoginLinkGenerator
import uk.ac.warwick.tabula.helpers.{FoundUser, Logging}
import uk.ac.warwick.util.web.filter.AbstractHttpFilter
import AcceptedUserSourceFilter._

object AcceptedUserSourceFilter {
  val whitelist: Set[String] = Set(
    "WarwickADS",
    "WarwickExtUsers",
    "WGAAluminati",
    // Implicit blacklist: WBSLdap, WBSAlumni, ResourceAccountsADS
  )

  def isWhitelistedUserSource(userSource: String): Boolean =
    userSource.isEmpty || whitelist.contains(userSource)
}

class AcceptedUserSourceFilter extends AbstractHttpFilter with Logging {
  override def doFilter(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain): Unit = {
    SSOClientFilter.getUserFromRequest(req) match {
      case FoundUser(u) if u.isLoggedIn && !isWhitelistedUserSource(u.getUserSource) =>
        logger.warn(s"Rejecting access for ${u.getUserId} as the user source isn't whitelisted (${u.getUserSource})")

        val generator = new SSOLoginLinkGenerator
        generator.setRequest(req)

        res.sendRedirect(generator.getPermissionDeniedLink)

      case _ =>
        chain.doFilter(req, res)
    }
  }
}
