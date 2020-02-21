package uk.ac.warwick.tabula.data.model

import enumeratum.{Enum, EnumEntry}
import uk.ac.warwick.tabula.system.EnumTwoWayConverter

import scala.collection.immutable

// full list of types taken from here - https://repo.elab.warwick.ac.uk/projects/MAPP/repos/app/browse/app/domain/fields/Assessment.scala#493-520
// TODO - fetch the list via an API call to module approval rather than maintaining it twice
sealed abstract class ExaminationType(override val entryName: String, val name: String, val description: Option[String] = None) extends EnumEntry
object ExaminationType extends Enum[ExaminationType] {
  case object Standard extends ExaminationType("STAN", "Standard", Some("A standard written exam paper, not seen in advance, with students not permitted to bring any texts."))
  case object SeenPaper extends ExaminationType("SEEN", "Seen paper", Some("Paper is available to candidates in advance."))
  case object OpenBook extends ExaminationType("OPEN", "Open book", Some("Students are permitted to bring any materials to the examination."))
  case object Restricted extends ExaminationType("REST", "Restricted", Some("Students are permitted to bring specific materials to the examination, eg annotated case study, 2 x A4 sides of handwritten notes, 2 x A4 sides of typed notes."))

  // Legacy?
  case object Sectioned extends ExaminationType("S", "Sectioned")
  case object NonSectioned extends ExaminationType("NS", "Not sectioned")
  case object OpenBookDictionary extends ExaminationType("OD", "Open book - dictionary")
  case object OpenBookRestricted extends ExaminationType("OR", "Open book restricted")
  case object OpenBookUnrestricted extends ExaminationType("OU", "Open book unrestricted")

  override def values: immutable.IndexedSeq[ExaminationType] = findValues
}

class ExaminationTypeUserType extends EnumUserType(ExaminationType)
class ExaminationTypeConverter extends EnumTwoWayConverter(ExaminationType)
