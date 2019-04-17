package uk.ac.warwick.tabula.system

import enumeratum.EnumEntry
import uk.ac.warwick.tabula.data.EnumCompanionHelper
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.language.reflectiveCalls
import scala.reflect.ClassTag
import scala.reflect.runtime.universe

abstract class EnumSeqTwoWayConverter[A >: Null <: EnumEntry](implicit tt: universe.TypeTag[A], ct: ClassTag[A]) extends TwoWayConverter[String, A]
  with EnumCompanionHelper {
  override def convertRight(code: String): A = if (code.hasText) companionOf[A].withName(code) else null
  override def convertLeft(enumEntry: A): String = Option(enumEntry).map(_.entryName).orNull
}