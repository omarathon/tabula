package uk.ac.warwick.tabula.data.model.groups

import java.sql.Types

import org.hibernate.`type`.StandardBasicTypes
import uk.ac.warwick.tabula.data.model.AbstractBasicUserType

sealed abstract class SmallGroupMembershipStyle(val dbValue: String, val description: String)

object SmallGroupMembershipStyle {

  case object SitsQuery extends SmallGroupMembershipStyle("SitsQuery", "SITS query")

  case object AssessmentComponents extends SmallGroupMembershipStyle("AssessmentComponents", "Assessment components")

  val Default = SitsQuery

  // lame manual collection. Keep in sync with the case objects above
  // Don't change this to a val https://warwick.slack.com/archives/C029QTGBN/p1493995125972397
  def members = Seq(SitsQuery, AssessmentComponents)

  def fromDatabase(dbValue: String): SmallGroupMembershipStyle = {
    if (dbValue == null) null
    else members.find {
      _.dbValue == dbValue
    } match {
      case Some(caseObject) => caseObject
      case None => throw new IllegalArgumentException()
    }
  }

  def apply(value: String): SmallGroupMembershipStyle = fromDatabase(value)
}

class SmallGroupMembershipStyleUserType extends AbstractBasicUserType[SmallGroupMembershipStyle, String] {

  val basicType = StandardBasicTypes.STRING

  override def sqlTypes = Array(Types.VARCHAR)

  val nullValue = null
  val nullObject = null

  override def convertToObject(string: String): SmallGroupMembershipStyle = SmallGroupMembershipStyle.fromDatabase(string)

  override def convertToValue(method: SmallGroupMembershipStyle): String = method.dbValue
}