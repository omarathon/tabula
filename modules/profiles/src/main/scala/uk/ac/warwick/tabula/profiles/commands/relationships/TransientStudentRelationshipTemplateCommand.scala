package uk.ac.warwick.tabula.profiles.commands.relationships

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Member, Department, StudentRelationshipType}
import uk.ac.warwick.tabula.helpers.{LazyMaps, LazyLists}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.views.ExcelView
import collection.JavaConverters._

/**
 * Generates a template spreadsheet for agent upload based on the POSTed values
 * rather than the persisted existing relationships
 */
object TransientStudentRelationshipTemplateCommand {
	def apply(department: Department, relationshipType: StudentRelationshipType) =
		new TransientStudentRelationshipTemplateCommandInternal(department, relationshipType)
			with AutowiringProfileServiceComponent
			with ComposableCommand[ExcelView]
			with TransientStudentRelationshipTemplatePermissions
			with TransientStudentRelationshipTemplateCommandState
			with ReadOnly with Unaudited
}


class TransientStudentRelationshipTemplateCommandInternal(val department: Department, val relationshipType: StudentRelationshipType)
	extends CommandInternal[ExcelView] with GeneratesStudentRelationshipWorkbook {

	self: TransientStudentRelationshipTemplateCommandState with ProfileServiceComponent =>

	override def applyInternal() = {
		// Transform into a list of (Member, Seq[Member]) pairs
		val existingAllocations =
			mapping.asScala.toSeq.flatMap{case(agent, students) => students.asScala.map((agent, _))}
				.groupBy(_._2)
				.toSeq
				.map{case(student, agentStudentPairs) => student -> agentStudentPairs.map(_._1)}

		val allAllocations =
			(existingAllocations ++ unallocated.asScala.map {(_, Nil)})
				.sortBy { case (student, _) => student.lastName + ", " + student.firstName}

		additionalAgents.asScala
			.flatMap { profileService.getAllMembersWithUserId(_) }
			.foreach { member =>
			if (!mapping.containsKey(member)) mapping.put(member, JArrayList())
		}

		val workbook = generateWorkbook(mapping.asScala.keys.toSeq, allAllocations, department, relationshipType)

		new ExcelView("Allocation for " + allocateSheetName(department, relationshipType) + ".xlsx", workbook)
	}

}

trait TransientStudentRelationshipTemplatePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: TransientStudentRelationshipTemplateCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(mandatory(relationshipType)), department)
	}

}

trait TransientStudentRelationshipTemplateCommandState {
	def department: Department
	def relationshipType: StudentRelationshipType

	// Bind variables

	var unallocated: JList[Member] = LazyLists.createWithFactory { () => null } // grower, not a shower
	var mapping: JMap[Member, JList[Member]] =
		LazyMaps.create { key: Member => JArrayList(): JList[Member] }.asJava
	var additionalAgents: JList[String] = JArrayList()
}
