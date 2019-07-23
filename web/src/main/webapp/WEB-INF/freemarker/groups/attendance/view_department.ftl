<#escape x as x?html>
  <#import "*/group_components.ftl" as components />
  <#import "/WEB-INF/freemarker/modal_macros.ftlh" as modal />

  <#function route_function dept>
    <#local result><@routes.groups.departmentAttendance dept adminCommand.academicYear /></#local>
    <#return result />
  </#function>
  <@fmt.id7_deptheader title="Attendance" route_function=route_function preposition="for"/>

  <#if !modules?has_content>
    <p class="alert alert-info empty-hint">This department doesn't have any groups set up.</p>
  <#else>
    <@components.department_attendance department modules adminCommand.academicYear />

  <#-- List of students modal -->
    <@modal.modal id="students-list-modal"></@modal.modal>
  </#if>

  <@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>
</#escape>