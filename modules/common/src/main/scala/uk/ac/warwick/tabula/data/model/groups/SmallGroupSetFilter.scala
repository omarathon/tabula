package uk.ac.warwick.tabula.data.model.groups

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model

sealed trait SmallGroupSetFilter {
	def description: String
	val getName = SmallGroupSetFilters.shortName(getClass.asInstanceOf[Class[_ <: SmallGroupSetFilter]])
	def apply(set: SmallGroupSet): Boolean
}

object SmallGroupSetFilters {
	private val ObjectClassPrefix = SmallGroupSetFilters.getClass.getName

	def shortName(clazz: Class[_ <: SmallGroupSetFilter])
		= clazz.getName.substring(ObjectClassPrefix.length, clazz.getName.length - 1).replace('$', '.')

	def of(name: String): SmallGroupSetFilter = {
		try {
			// Go through the magical hierarchy
			val clz = Class.forName(ObjectClassPrefix + name.replace('.', '$') + "$")
			clz.getDeclaredField("MODULE$").get(null).asInstanceOf[SmallGroupSetFilter]
		} catch {
			case e: ClassNotFoundException => throw new IllegalArgumentException("SmallGroupSetFilter " + name + " not recognised")
			case e: ClassCastException => throw new IllegalArgumentException("SmallGroupSetFilter " + name + " is not an endpoint of the hierarchy")
		}
	}

	case class Module(module: model.Module) extends SmallGroupSetFilter {
		val description = module.code.toUpperCase() + " " + module.name
		override val getName = "Module(" + module.code + ")"
		def apply(set: SmallGroupSet) = set.module == module
	}

	def allModuleFilters(modules: Seq[model.Module]) = modules.map { Module(_) }

	object Status {
		case object ContainsGroups extends SmallGroupSetFilter {
			val description = "Contains groups"
			def apply(set: SmallGroupSet) = set.groups.asScala.nonEmpty
		}
		case object UnallocatedStudents extends SmallGroupSetFilter {
			val description = "Unallocated students"
			def apply(set: SmallGroupSet) = set.unallocatedStudentsCount > 0
		}
		case object OpenForSignUp extends SmallGroupSetFilter {
			val description = "Open for sign up"
			def apply(set: SmallGroupSet) = set.openForSignups
		}
		case object NeedsNotificationsSending extends SmallGroupSetFilter {
			val description = "Needs notifications sending"
			def apply(set: SmallGroupSet) = !set.fullyReleased
		}

		val all = Seq(ContainsGroups, UnallocatedStudents, OpenForSignUp, NeedsNotificationsSending)
	}

	object AllocationMethod {
		case object ManuallyAllocated extends SmallGroupSetFilter {
			val description = "Manually allocated"
			def apply(set: SmallGroupSet) = set.allocationMethod == SmallGroupAllocationMethod.Manual
		}
		case object StudentSignUp extends SmallGroupSetFilter {
			val description = "Self sign-up"
			def apply(set: SmallGroupSet) = set.allocationMethod == SmallGroupAllocationMethod.StudentSignUp
		}
		case class Linked(linked: DepartmentSmallGroupSet) extends SmallGroupSetFilter {
			val description = "Linked to " + linked.name
			override val getName = "AllocationMethod.Linked(" + linked.id + ")"
			def apply(set: SmallGroupSet) = set.linked && set.linkedDepartmentSmallGroupSet == linked
		}

		def all(linked: Seq[DepartmentSmallGroupSet]) = Seq(ManuallyAllocated, StudentSignUp) ++ linked.map { Linked(_) }
	}
}
