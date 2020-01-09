package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.helpers.cm2.AssignmentSubmissionStudentInfo
import uk.ac.warwick.tabula.web.controllers.MessageResolver

trait AssignmentStudentMessageResolver {
  def getMessageForStudent(student: AssignmentSubmissionStudentInfo, key: String, args: Object*): String
}

trait ReplacingAssignmentStudentMessageResolver extends AssignmentStudentMessageResolver {
  self: MessageResolver =>

  def getMessageForStudent(student: AssignmentSubmissionStudentInfo, key: String, args: Object*): String = {
    val studentId = student.user.getWarwickId

    getMessage(key, args)
      .replace("[STUDENT]", studentId)
  }
}
