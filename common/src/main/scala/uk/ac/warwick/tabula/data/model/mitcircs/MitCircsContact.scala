package uk.ac.warwick.tabula.data.model.mitcircs


import enumeratum.{EnumEntry, _}
import uk.ac.warwick.tabula.data.model.EnumSeqUserType
import uk.ac.warwick.tabula.system.EnumTwoWayConverter

import scala.collection.immutable

sealed abstract class MitCircsContact(val description: String) extends EnumEntry

object MitCircsContact extends Enum[MitCircsContact] {

  val values: immutable.IndexedSeq[MitCircsContact] = findValues

  case object PersonalTutor extends MitCircsContact(description = "Personal tutor")
  case object SeniorTutor extends MitCircsContact(description = "Departmental senior tutor")
  case object FacultySeniorTutor extends MitCircsContact(description = "Faculty senior tutor")
  case object WSS extends MitCircsContact(description = "Wellbeing support services")
  case object DirectorOfStudies extends MitCircsContact(description = "Director of UG or PG studies / Course leader")
  case object StudentsUnion extends MitCircsContact(description = "Students’ Union advice centre")
  case object ResidentialSupport extends MitCircsContact(description = "Residential support")
  case object UniversityCounsellingService extends MitCircsContact(description = "University counselling service")
  case object Doctor extends MitCircsContact(description = "Doctor / NHS services")
  case object Other extends MitCircsContact(description = "Other")
}

class MitCircsContactUserType extends EnumSeqUserType(MitCircsContact)
class MitCircsContactConverter extends EnumTwoWayConverter(MitCircsContact)
