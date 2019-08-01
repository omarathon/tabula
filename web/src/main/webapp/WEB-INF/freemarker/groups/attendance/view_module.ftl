<#escape x as x?html>
  <#import "*/group_components.ftlh" as components />
  <#import "/WEB-INF/freemarker/modal_macros.ftlh" as modal />

  <h1>Attendance for <@fmt.module_name module /></h1>

  <#if nonempty(sets?keys)>
    <#list sets?keys as set>
      <#assign groups = mapGet(sets, set) />

      <@components.single_groupset_attendance set groups />
    </#list>

  <#-- List of students modal -->
    <@modal.modal id="students-list-modal"></@modal.modal>
  <#else>
    <p>There are no small group events for <@fmt.module_name module false /></p>
  </#if>

  <@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>
</#escape>