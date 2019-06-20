
<#if assignment.collectSubmissions>
<@fmt.p number=allocatedStudentsCount!0 singular="student" plural="students" /> <@fmt.p number=allocatedStudentsCount!0 singular="is" plural="are" shownumber=false /> allocated to you for marking.
  <#list studentsAtStagesCount as info>
    <#if info.count gt 0>
- ${info.stageName}: <@fmt.p number=info.count!0 singular="student" plural="students" />
     </#if>
  </#list>

  <#if feedbacksCount! gt 0>
<@fmt.p number=feedbacksCount!0 singular="student" plural="students" /> allocated to you <@fmt.p number=feedbacksCount!0 singular="has" plural="have" shownumber=false /> been released for marking<#if assignment.automaticallyReleaseToMarkers!false> as the assignment has been set to automatically release when the end date and time have been reached</#if>:
- <@fmt.p number=submissionsCount!0 singular="student" plural="students" /> <@fmt.p number=submissionsCount!0 singular="has" plural="have" shownumber=false /> submitted work that can be marked
- <@fmt.p number=noSubmissionsWithExtensionCount!0 singular="student" plural="students" /> <@fmt.p number=noSubmissionsWithExtensionCount!0 singular="has" plural="have" shownumber=false /> not submitted but they have an extension
- <@fmt.p number=noSubmissionsWithoutExtensionCount!0 singular="student" plural="students" /> <@fmt.p number=noSubmissionsWithoutExtensionCount!0 singular="has" plural="have" shownumber=false /> not submitted work and <@fmt.p number=noSubmissionsWithoutExtensionCount!0 singular="has" plural="have" shownumber=false /> not yet requested an extension
  <#else>
      <#if assignment.automaticallyReleaseToMarkers!false>
No students allocated to you have been released for marking as the assignment has been set to automatically release when the end date and time have been reached but no students have yet submitted.

Please check the assignment regularly as students with extensions may submit at any time.
      </#if>
  </#if>
<#else>
<@fmt.p number=feedbacksCount!0 singular="student" plural="students" /> <@fmt.p number=feedbacksCount!0 singular="is" plural="are" shownumber=false /> allocated to you for marking.
- This assignment does not require students to submit work to Tabula
</#if>