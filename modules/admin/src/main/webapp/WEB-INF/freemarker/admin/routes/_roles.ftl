<#import "/WEB-INF/freemarker/permissions_macros.ftl" as pm />
<#import "/WEB-INF/freemarker/formatters.ftl" as fmt />

<#escape x as x?html>
	<div class="row-fluid">
		<div class="span6">
			<#assign popover>
				<p>A route manager can manage monitoring points for a route and record which students have attended or missed points.</p>
			</#assign>

			<h3 class="permissionTitle">Route Managers</h3> <a class="use-popover" id="popover-routemanager" data-html="true"
			   data-original-title="Route Managers"
			   data-content="${popover}"><i class="icon-question-sign"></i></a>

			<@pm.roleTable perms_url "manager-table" scope "RouteManagerRoleDefinition" "route managers" />
		</div>

		<div class="span6">
			<#assign popover>
				<p>A route assistant can record whether students have attended or missed a monitoring point, but can't change the monitoring point schemes.</p>
			</#assign>

			<h3 class="permissionTitle">Route Assistants</h3> <a class="use-popover" id="popover-routeassistant" data-html="true"
			   data-original-title="Route Assistants"
			   data-content="${popover}"><i class="icon-question-sign"></i></a>

			<@pm.roleTable perms_url "assistant-table" scope "RouteAssistantRoleDefinition" "route assistants" />
		</div>
	</div>
	
	<div class="row-fluid">
		<div class="span6">
			<#assign popover>
				<p>A route auditor can view which students have met or missed monitoring points.</p>
			</#assign>

			<h3 class="permissionTitle">Route Auditors</h3> <a class="use-popover" id="popover-routeauditor" data-html="true"
			   data-original-title="Route Auditors"
			   data-content="${popover}"><i class="icon-question-sign"></i></a>

			<@pm.roleTable perms_url "auditor-table" scope "RouteAuditorRoleDefinition" "route auditors" />
		</div>
	</div>
</#escape>