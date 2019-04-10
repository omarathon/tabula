package uk.ac.warwick.tabula.mitcircs.web

import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.web.RoutesUtils

/**
  * Generates URLs to various locations, to reduce the number of places where URLs
  * are hardcoded and repeated.
  *
  * For methods called "apply", you can leave out the "apply" and treat the object like a function.
  */
object Routes {

  import RoutesUtils._

  private val context = "/mitcircs"

  def home: String = context + "/"

  object Admin {
    def home(department: Department): String = context + "/admin/%s" format encoded(department.code)
    def review(submission: MitigatingCircumstancesSubmission): String = context + "/admin/review/%s" format encoded(submission.key.toString)
  }

  object Student {
    def home: String = context + "/profile"
    def home(student: StudentMember): String = home + "/%s" format encoded(student.universityId)
  }

}
