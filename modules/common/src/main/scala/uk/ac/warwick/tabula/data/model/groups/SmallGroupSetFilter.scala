package uk.ac.warwick.tabula.data.model.groups

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model

sealed trait SmallGroupSetFilter {
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
		override val getName = SmallGroupSetFilters.shortName(getClass.asInstanceOf[Class[_ <: SmallGroupSetFilter]]) + "(" + module.code + ")"
		def apply(set: SmallGroupSet) = set.module == module
	}

	object Status {
		case object ContainsGroups extends SmallGroupSetFilter {
			def apply(set: SmallGroupSet) = set.groups.asScala.nonEmpty
		}
		case object UnallocatedStudents extends SmallGroupSetFilter {
			def apply(set: SmallGroupSet) = set.unallocatedStudentsCount > 0
		}
		case object OpenForSignUp extends SmallGroupSetFilter {
			def apply(set: SmallGroupSet) = set.openForSignups
		}
		case object NeedsNotificationsSending extends SmallGroupSetFilter {
			def apply(set: SmallGroupSet) = !set.fullyReleased
		}
	}

	object AllocationMethod {
		case object ManuallyAllocated extends SmallGroupSetFilter {
			def apply(set: SmallGroupSet) = set.allocationMethod == SmallGroupAllocationMethod.Manual
		}
		case object StudentSignUp extends SmallGroupSetFilter {
			def apply(set: SmallGroupSet) = set.allocationMethod == SmallGroupAllocationMethod.StudentSignUp
		}
		case class Linked(linked: DepartmentSmallGroupSet) extends SmallGroupSetFilter {
			override val getName = SmallGroupSetFilters.shortName(getClass.asInstanceOf[Class[_ <: SmallGroupSetFilter]]) + "(" + linked.id + ")"
			def apply(set: SmallGroupSet) = set.linked && set.linkedDepartmentSmallGroupSet == linked
		}
	}
}
