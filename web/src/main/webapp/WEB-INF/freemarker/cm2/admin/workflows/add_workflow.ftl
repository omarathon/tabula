<#escape x as x?html>

<#assign formAction><@routes.cm2.reusableWorkflowAdd department academicYear /></#assign>
<#assign commandName = "addMarkingWorkflowCommand" />
<#assign newRecord = true />

<#include "_modify_workflow.ftl" />

</#escape>