package uk.ac.warwick.tabula.data.model

import enumeratum.{Enum, EnumEntry}

sealed abstract class ModuleResult(val dbValue: String, val description: String) extends EnumEntry {
  override val entryName: String = dbValue
}

object ModuleResult extends Enum[ModuleResult] {
  case object Pass extends ModuleResult("P", "Pass")
  case object NoResult extends ModuleResult("X", "No result")
  case object Fail extends ModuleResult("F", "Fail")
  case object Deferred extends ModuleResult("D", "Deferred")

  // other values appear to exist in data such as R, A, M - these will be ignored without exploding and return null
  def fromCode(code: String): ModuleResult = withNameOption(code).orNull

  override def values: IndexedSeq[ModuleResult] = findValues
}

class ModuleResultUserType extends EnumUserType(ModuleResult)
class OptionModuleResultUserType extends OptionEnumUserType(ModuleResult)
