package uk.ac.warwick.tabula.data

import enumeratum.EnumEntry

import scala.reflect.runtime.universe

trait EnumCompanionHelper {

  private lazy val universeMirror = universe.runtimeMirror(getClass.getClassLoader)

  def companionOf[T <: EnumEntry](implicit tt: universe.TypeTag[T]): { def withName(s: String): T } = {
    val companionMirror = universeMirror.reflectModule(universe.typeOf[T].typeSymbol.companion.asModule)
    companionMirror.instance.asInstanceOf[{ def withName(s: String): T }]
  }

}
