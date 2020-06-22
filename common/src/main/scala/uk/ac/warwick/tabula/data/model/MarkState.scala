package uk.ac.warwick.tabula.data.model

import enumeratum.{Enum, EnumEntry}
import uk.ac.warwick.tabula.system.EnumTwoWayConverter

import scala.collection.immutable

sealed abstract class MarkState(val description: String, val cssClass: String) extends EnumEntry
object MarkState extends Enum[MarkState] {
  override val values: IndexedSeq[MarkState] = findValues

  case object UnconfirmedActual extends MarkState("Unconfirmed actual", "default")
  case object ConfirmedActual extends MarkState("Confirmed actual", "info") // could be named Actual but being explicit also helps with Actual/Agreed confusion
  case object Agreed extends MarkState("Agreed", "success")
}

class MarkStateUserType extends EnumUserType(MarkState)
class MarkStateConverter extends EnumTwoWayConverter(MarkState)
