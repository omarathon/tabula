package uk.ac.warwick.tabula.data.model

import java.sql.Types

import org.hibernate.`type`.StandardBasicTypes

sealed abstract class ModuleResult(val dbValue: String, val description: String)

object ModuleResult {

  case object Pass extends ModuleResult("P", "Pass")

  case object Fail extends ModuleResult("F", "Fail")

  case object Deferred extends ModuleResult("D", "Deferred")

  def fromCode(code: String): ModuleResult = code match {
    case Pass.dbValue => Pass
    case Fail.dbValue => Fail
    case Deferred.dbValue => Deferred
    case _ => null // other values appear to exist in data such as R, A, M - these will be ignored without exploding
  }
}

class ModuleResultUserType extends AbstractBasicUserType[ModuleResult, String] {

  val basicType = StandardBasicTypes.STRING

  override def sqlTypes = Array(Types.VARCHAR)

  val nullValue = null
  val nullObject = null

  override def convertToObject(string: String): ModuleResult = ModuleResult.fromCode(string)

  override def convertToValue(result: ModuleResult): String = result.dbValue

}

class OptionModuleResultUserType extends AbstractBasicUserType[Option[ModuleResult], String] {
  val basicType = StandardBasicTypes.STRING
  override def sqlTypes = Array(Types.VARCHAR)
  val nullValue = null
  val nullObject: Option[ModuleResult] = None
  override def convertToObject(string: String): Option[ModuleResult] = Option(string).map(ModuleResult.fromCode)
  def convertToValue(obj: Option[ModuleResult]): String = obj.map(_.dbValue).orNull
}
