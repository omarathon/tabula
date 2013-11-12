<#compress><#escape x as x?html>

<#import "/WEB-INF/freemarker/permissions_macros.ftl" as pm />
<#import "/WEB-INF/freemarker/formatters.ftl" as fmt />
<#assign moduleperms_url><@routes.moduleperms module /></#assign>
<#assign module_name><@fmt.module_name module /></#assign>

<div id="module-permissions-page">
	<h1>Module permissions</h1>
	<h5>for <#noescape>${module_name}</#noescape></h5>

	<@pm.alerts "addCommand" module_name users role />

	<div class="row-fluid">
		<div class="span6">
			<#assign popover>
				<p>A module manager can create and delete assignments, download submissions and publish feedback.</p>
			</#assign>

			<h3 class="permissionTitle">Module Managers</h3> <a class="use-popover" id="popover-modulemanager" data-html="true"
			   data-original-title="Module Managers"
			   data-content="${popover}"><i class="icon-question-sign"></i></a>

			<@pm.roleTable moduleperms_url "manager-table" module "ModuleManagerRoleDefinition" "module managers" />
		</div>

		<div class="span6">
			<#assign popover>
				<p>A module assistant can create assignments and download submissions, but cannot delete assignments or submissions, or publish feedback.</p>
			</#assign>

			<h3 class="permissionTitle">Module Assistants</h3> <a class="use-popover" id="popover-moduleassistant" data-html="true"
			   data-original-title="Module Assistants"
			   data-content="${popover}"><i class="icon-question-sign"></i></a>

			<@pm.roleTable moduleperms_url "assistant-table" module "ModuleAssistantRoleDefinition" "module assistants" />
		</div>
	</div>
	
	<div class="row-fluid">
		<div class="span6">
			<#assign popover>
				<p>A module auditor can view submissions, feedback and marks, but cannot change any information in Tabula.</p>
			</#assign>

			<h3 class="permissionTitle">Module Auditors</h3> <a class="use-popover" id="popover-moduleauditor" data-html="true"
			   data-original-title="Module Auditors"
			   data-content="${popover}"><i class="icon-question-sign"></i></a>

			<@pm.roleTable moduleperms_url "auditor-table" module "ModuleAuditorRoleDefinition" "module auditors" />
		</div>
	</div>
</div>

<@pm.script />

</#escape></#compress>