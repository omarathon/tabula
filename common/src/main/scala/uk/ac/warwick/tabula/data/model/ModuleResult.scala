package uk.ac.warwick.tabula.data.model

import enumeratum.{Enum, EnumEntry}

sealed abstract class ModuleResult(val dbValue: String, val description: String) extends EnumEntry {
  override val entryName: String = dbValue
}

object ModuleResult extends Enum[ModuleResult] {
  case object Pass extends ModuleResult("P", "Pass")
  case object Fail extends ModuleResult("F", "Fail")
  case object Deferred extends ModuleResult("D", "No result")

  def fromCode(code: String): ModuleResult = withNameOption(code).orNull
  override def values: IndexedSeq[ModuleResult] = findValues
}

class ModuleResultUserType extends EnumUserType(ModuleResult)
class OptionModuleResultUserType extends OptionEnumUserType(ModuleResult)
