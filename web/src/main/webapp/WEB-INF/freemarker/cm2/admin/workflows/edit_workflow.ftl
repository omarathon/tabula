<#import "*/cm2_macros.ftl" as cm2 />
<#escape x as x?html>

<#assign formAction><@routes.cm2.reusableWorkflowEdit department academicYear workflow/></#assign>
<#assign commandName = "editMarkingWorkflowCommand" />
<#assign newRecord = false />

<#include "_modify_workflow.ftl" />

</#escape>