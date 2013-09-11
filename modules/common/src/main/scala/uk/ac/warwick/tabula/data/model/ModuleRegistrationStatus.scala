package uk.ac.warwick.tabula.data.model

import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

sealed abstract class ModuleRegistrationStatus(val dbValue: String, val description: String)

object ModuleRegistrationStatus {
	// These are the possible values in SITS for SSN_MRG; the first is one I've added to cover the case where there is no SSN row
	case object NYG extends ModuleRegistrationStatus(null, "None")
	case object GEN extends ModuleRegistrationStatus("GEN", "Ready for student to make choices")
	case object ENT extends ModuleRegistrationStatus("ENT", "Student has made choices; awaiting confirmation")
	case object CON extends ModuleRegistrationStatus("CON", "Confirmed")
	case object PCO extends ModuleRegistrationStatus("PCO", "Previously confirmed")
	case object QUE extends ModuleRegistrationStatus("QUE", "Queried")
	case object ANS extends ModuleRegistrationStatus("ANS", "Query answered")
	case object REJ extends ModuleRegistrationStatus("REJ", "Rejected")
	case object MRJ extends ModuleRegistrationStatus("MRJ", "Module rejected")
	case object NOTAPP extends ModuleRegistrationStatus("NOTAPP", "Not available")
	case object CRQ extends ModuleRegistrationStatus("CRQ", "Change requested")
	case object CRJ extends ModuleRegistrationStatus("CRJ", "Change rejected")
	case object PCQ extends ModuleRegistrationStatus("PCQ", "Post-confirmation query")
	case object PCA extends ModuleRegistrationStatus("PCA", "Post-confirmation answer")

	def fromCode(code: String) = code match {
	  	case NYG.dbValue => NYG
	  	case GEN.dbValue => GEN
	  	case ENT.dbValue => ENT
	  	case CON.dbValue => CON
	  	case PCO.dbValue => PCO
	  	case QUE.dbValue => QUE
	  	case ANS.dbValue => ANS
	  	case REJ.dbValue => REJ
	  	case MRJ.dbValue => MRJ
	  	case NOTAPP.dbValue => NOTAPP
	  	case CRQ.dbValue => CRQ
	  	case CRJ.dbValue => CRJ
	  	case PCQ.dbValue => PCQ
	  	case PCA.dbValue => PCA
	  	case null => NYG
	  	case _ => throw new IllegalArgumentException()
	}
}

class ModuleRegistrationStatusUserType extends AbstractBasicUserType[ModuleRegistrationStatus, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String) = ModuleRegistrationStatus.fromCode(string)

	override def convertToValue(selectionStatus: ModuleRegistrationStatus) = selectionStatus.dbValue

}