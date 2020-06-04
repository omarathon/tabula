package uk.ac.warwick.tabula.data.model

import enumeratum.{Enum, EnumEntry}
import uk.ac.warwick.tabula.system.EnumTwoWayConverter

import scala.collection.immutable

sealed abstract class MarkState extends EnumEntry

object MarkState extends Enum[MarkState] {

  val values: immutable.IndexedSeq[MarkState] = findValues

  case object UnconfirmedActual extends MarkState
  case object ConfirmedActual extends MarkState // could be named Actual but being explicit also helps with Actual/Agreed confusion
  case object Agreed extends MarkState
}

class MarkStateUserType extends EnumUserType(MarkState)
class MarkStateConverter extends EnumTwoWayConverter(MarkState)
