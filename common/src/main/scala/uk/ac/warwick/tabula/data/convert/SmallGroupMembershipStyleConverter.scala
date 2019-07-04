package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.data.model.groups.SmallGroupMembershipStyle
import uk.ac.warwick.tabula.system.TwoWayConverter

class SmallGroupMembershipStyleConverter extends TwoWayConverter[String, SmallGroupMembershipStyle] {

  override def convertRight(code: String): SmallGroupMembershipStyle = SmallGroupMembershipStyle.fromDatabase(code)

  override def convertLeft(membershipStyle: SmallGroupMembershipStyle): String = Option(membershipStyle).map(_.dbValue).orNull

}
