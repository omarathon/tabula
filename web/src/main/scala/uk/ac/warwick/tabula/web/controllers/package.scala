package uk.ac.warwick.tabula.web

import enumeratum.{Enum, EnumEntry}
import uk.ac.warwick.util.web.bind.AbstractPropertyEditor

package object controllers {
  def propertyEditor[A](
    convertFromString: String => A,
    convertToString: A => String
  ): AbstractPropertyEditor[A] = new AbstractPropertyEditor[A]() {
    override def toString(obj: A): String = convertToString(obj)
    override def fromString(id: String): A = convertFromString(id)
  }

  def enumPropertyEditor[E <: EnumEntry](enum: Enum[E]): AbstractPropertyEditor[E] =
    propertyEditor[E](enum.withName, _.entryName)
}
