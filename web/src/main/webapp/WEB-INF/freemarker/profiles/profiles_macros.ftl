<#escape x as x?html>

  <#macro assignmentLinks assignment>
    <h4>
        <#if can.do_as_real_user('Submission.Read', assignment.module)>
            <#assign module_url><@routes.cm2.depthome assignment.module /></#assign>
            <@fmt.module_name_with_link assignment.module module_url />
        <#else>
            <@fmt.module_name assignment.module />
        </#if>
    </h4>
    <h4>
        <#if can.do_as_real_user('Submission.Read', assignment.module)>
          <a href="<@routes.cm2.assignmentSubmissionSummary assignment />">
            <span class="ass-name">${assignment.name}</span>
          </a>
        <#else>
          <span class="ass-name">${assignment.name}</span>
        </#if>
    </h4>
  </#macro>

</#escape>
