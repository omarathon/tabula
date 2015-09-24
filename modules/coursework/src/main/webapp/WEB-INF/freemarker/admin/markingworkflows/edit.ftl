<div class="pull-right">
	<a class="btn btn-default" href="<@routes.markingworkflowreplace command.markingWorkflow />">Replace marker</a>
</div>

<#assign view_type="edit" />
<#assign form_url><@routes.markingworkflowedit command.markingWorkflow /></#assign>
<#include "_form.ftl" />