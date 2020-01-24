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

  <#macro profile_header member isSelf>
    <#if !isSelf>
      <#if member.deceased>
        <div class="alert alert-danger" role="alert">
          <p class="lead"><i class="fas fa-exclamation-triangle"></i> This student is recorded as deceased in SITS</p>
        </div>
      </#if>

      <details class="indent">
        <summary>${member.fullName}</summary>
        <#if member.userId??>
          ${member.userId}<br />
        </#if>
        <#if member.email??>
          <a href="mailto:${member.email}">${member.email}</a><br />
        </#if>
        <#if member.phoneNumber??>
          ${phoneNumberFormatter(member.phoneNumber)}<br />
        </#if>
        <#if member.mobileNumber??>
          ${phoneNumberFormatter(member.mobileNumber)}<br />
        </#if>
      </details>
    </#if>
  </#macro>

</#escape>
