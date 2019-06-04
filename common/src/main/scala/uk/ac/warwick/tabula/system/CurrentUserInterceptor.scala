package uk.ac.warwick.tabula.system

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.springframework.validation.BindException
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.sso.client.SSOClientFilter
import uk.ac.warwick.tabula.commands.admin.MasqueradeCommand
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.helpers.FoundUser
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.roles.{Masquerader, Sysadmin}
import uk.ac.warwick.tabula.services.permissions.RoleService
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, ProfileService}
import uk.ac.warwick.tabula.web.Cookies._
import uk.ac.warwick.tabula.{CurrentUser, NoCurrentUser}
import uk.ac.warwick.userlookup.{User, UserLookupInterface}

class CurrentUserInterceptor extends HandlerInterceptorAdapter {
  var roleService: RoleService = Wire[RoleService]
  var userLookup: UserLookupInterface = Wire[UserLookupInterface]
  var profileService: ProfileService = Wire[ProfileService]
  var departmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]
  var userNavigationGenerator: UserNavigationGenerator = UserNavigationGeneratorImpl

  type MasqueradeUserCheck = (User, Boolean) => User
  type MasqueradeRequestUsercodeExtractor = () => Option[String]

  def resolveCurrentUser(user: User, masqueradeUser: MasqueradeUserCheck, godModeEnabled: => Boolean): CurrentUser = transactional(readOnly = true) {
    val sysadmin = roleService.hasRole(new CurrentUser(user, user), Sysadmin())
    val god = sysadmin && godModeEnabled
    val masquerader =
      sysadmin ||
        roleService.hasRole(new CurrentUser(user, user), Masquerader()) ||
        departmentService.departmentsWithPermission(new CurrentUser(user, user), Permissions.Masquerade).nonEmpty
    val canMasquerade = sysadmin || masquerader
    val apparentUser = masqueradeUser(user, canMasquerade)

    new CurrentUser(
      realUser = user,
      apparentUser = apparentUser,
      profile = profileService.getMemberByUser(user = apparentUser, disableFilter = true, eagerLoad = true),
      sysadmin = sysadmin,
      masquerader = masquerader,
      god = god,
      navigation = userNavigationGenerator(apparentUser)
    )
  }

  override def preHandle(request: HttpServletRequest, response: HttpServletResponse, obj: Any): Boolean = {
    def extractMasqueradeUsercodeFromCookie(): Option[String] =
      request.getCookies.getString(CurrentUser.masqueradeCookie).filter(_.hasText)

    val currentUser: CurrentUser = request.getAttribute(SSOClientFilter.USER_KEY) match {
      case FoundUser(user) =>
        // If someone has passed a masquerade user as a query parameter rather than a Cookie, set the equivalent Cookie
        // and use that value instead
        val extractionMethod: MasqueradeRequestUsercodeExtractor =
          if (extractMasqueradeUsercodeFromCookie().isEmpty && request.getParameter(CurrentUser.masqueradeCookie).hasText) transactional(readOnly = true) {
            // For auditability
            val cmd = MasqueradeCommand(new CurrentUser(user, user))
            cmd.usercode = request.getParameter(CurrentUser.masqueradeCookie)

            val errors = new BindException(cmd, "masqueradeCommand")
            cmd.validate(errors)

            if (!errors.hasErrors) {
              val cookie = cmd.apply()
              cookie.foreach { c =>
                response.addCookie(c.cookie)
              }
              () => Some(cmd.usercode)
            } else () => extractMasqueradeUsercodeFromCookie()
          } else () => extractMasqueradeUsercodeFromCookie()

        resolveCurrentUser(user, apparentUser(extractionMethod), godCookieExists(request))

      case _ => NoCurrentUser()
    }
    request.setAttribute(CurrentUser.keyName, currentUser)
    true //allow request to continue
  }

  private def godCookieExists(request: HttpServletRequest): Boolean =
    request.getCookies.getBoolean(CurrentUser.godModeCookie, default = false)

  // masquerade support
  private def apparentUser(extractor: MasqueradeRequestUsercodeExtractor)(realUser: User, canMasquerade: Boolean): User =
    if (canMasquerade) {
      extractor() match {
        case Some(userid) => userLookup.getUserByUserId(userid) match {
          case user: User if user.isFoundUser => user
          case _ => realUser
        }
        case None => realUser
      }
    } else {
      realUser
    }

}