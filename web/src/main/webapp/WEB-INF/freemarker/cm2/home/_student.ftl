<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>

  <h1>Assignments</h1>

  <@components.all_student_assignment_lists studentInformation=studentInformation isSelf=true />

</#escape>
