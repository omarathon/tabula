<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>

<#-- Do we expect this user to submit assignments, and therefore show them some text even if there aren't any? -->
  <#assign expect_assignments = user.student || user.PGR || user.alumni />
  <#assign is_student = expect_assignments || !studentInformation.empty />
  <#assign is_marker = markingAcademicYears?has_content />
  <#assign is_admin = !adminInformation.empty />

  <#if is_student>
    <#include "_student.ftl" />
  </#if>

  <#if is_marker>
    <h1>Assignments for marking</h1>

    <#if !activeAcademicYear?? || !markingAcademicYears?seq_contains(activeAcademicYear)>
      <#assign activeAcademicYear = markingAcademicYears?last />
    </#if>

    <#if markingAcademicYears?size gt 1>
      <ul class="nav nav-tabs" role="tablist">
        <#list markingAcademicYears as academicYear>
          <li role="presentation"<#if academicYear.startYear = (activeAcademicYear.startYear)!0> class="active"</#if>><a href="#marking-${academicYear.startYear?c}" aria-controls="marking-${academicYear.startYear?c}" data-toggle="tab">${academicYear.toString}</a></li>
        </#list>
      </ul>
    </#if>

    <div class="tab-content">
      <#list markingAcademicYears as academicYear>
        <div role="tabpanel" class="tab-pane<#if academicYear.startYear = (activeAcademicYear.startYear)!0> active</#if>" id="marking-${academicYear.startYear?c}">
          <p class="hint"><i class="id7-koan-spinner id7-koan-spinner--xs id7-koan-spinner--inline" aria-hidden="true"></i> Loading&hellip;</p>
        </div>
      </#list>

      <script type="text/javascript" nonce="${nonce()}">
        (function ($) {
          <#list markingAcademicYears as academicYear>
            $('#marking-${academicYear.startYear?c}').load('<@routes.cm2.markerHomeForYear academicYear />');
          </#list>
        })(jQuery);
      </script>
    </div>
  </#if>

  <#if is_admin || is_marker> <#-- Markers get the activity stream -->
    <#include "_admin.ftl" />
  </#if>

  <#include "_marks_management_admin.ftlh" />

  <#if !is_student && !is_marker && !is_admin>
    <h1>Coursework Management</h1>

    <p class="lead muted">
      This is a service for managing coursework assignments and feedback.
    </p>

    <p>
      <#if homeDepartment??>
        <#assign uams = usersWithRole('UserAccessMgrRoleDefinition', homeDepartment) />
      </#if>
      You do not currently have permission to manage any assignments or feedback. If you think this is incorrect or you need assistance, please
      <#if uams?has_content>
        contact your department's <a href="mailto:${uams?first.email}">User Access Manager</a> for Tabula or
      </#if>
      visit our <a href="/help">help page</a>.
    </p>
  </#if>

</#escape>
