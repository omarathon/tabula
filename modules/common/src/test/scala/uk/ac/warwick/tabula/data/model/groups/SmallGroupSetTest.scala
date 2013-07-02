package uk.ac.warwick.tabula.data.model.groups

import uk.ac.warwick.tabula.{Mockito, AcademicYear, TestBase}
import org.junit.Test
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.{UpstreamAssessmentGroup, Module, UserGroup}
import uk.ac.warwick.tabula.services.AssignmentMembershipService
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import scala.collection.JavaConverters._
import org.mockito.Mockito._

class SmallGroupSetTest extends TestBase with Mockito{

  @Test
  def duplicateCopiesAllFields(){
    val source = new SmallGroupSet
    val cloneGroup = new SmallGroup
    val group:SmallGroup  = new SmallGroup(){
      override def duplicateTo(groupSet:SmallGroupSet) = cloneGroup
    }
    source.id = "testId"
    source.academicYear = AcademicYear(2001)
    source.allocationMethod = SmallGroupAllocationMethod.Manual
    source.archived = true
    source.assessmentGroups =  JArrayList()
    source.format = SmallGroupFormat.Lab
    source.groups  = JArrayList(group)
    source.members =  mock[UserGroup]
    source.membershipService = mock[AssignmentMembershipService]
    source.module = new Module
    source.name = "test name"
    source.permissionsService = mock[PermissionsService]
    source.releasedToStudents = true
    source.releasedToTutors = true
    val cloneModule = new Module
    val assessmentGroups:JList[UpstreamAssessmentGroup] = JArrayList()


    val clone = source.duplicateTo(cloneModule)

    clone.id should be(source.id)
    clone.academicYear should be (source.academicYear)
    clone.allocationMethod should be (source.allocationMethod)
    clone.archived should be(source.archived)
    clone.assessmentGroups should be(assessmentGroups)
    clone.format should be (source.format)
    clone.groups.size should be(1)
    clone.groups.asScala.head should be(cloneGroup)
    verify(source.members, times(1)).duplicate()
    clone.module should be (cloneModule)
    clone.name should be(source.name)
    clone.permissionsService should be(source.permissionsService)
    clone.releasedToStudents should be(source.releasedToStudents)
    clone.releasedToTutors should be (source.releasedToTutors)
  }
}
