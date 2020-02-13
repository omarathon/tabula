<#import "*/profiles_macros.ftl" as profiles />
<#import "/WEB-INF/freemarker/cm2/coursework_components.ftl" as components />

<#escape x as x?html>

  <@profiles.profile_header member isSelf />

  <h1>Assignments</h1>

  <#if hasPermission>
    <@components.all_student_assignment_lists studentInformation />
  <#else>
    <div class="alert alert-info">
      You do not have permission to see the assignments for this course.
    </div>
  </#if>


</#escape>
