package uk.ac.warwick.tabula.data.model

import org.hibernate.`type`.StandardBasicTypes
import scala.Array
import java.sql.Types
import uk.ac.warwick.tabula.data.model.SubmissionState._

class SubmissionStateUserType extends AbstractBasicUserType[SubmissionState, String]{

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String) = SubmissionState.withName(string)
	override def convertToValue(state: SubmissionState) = state.toString
}
