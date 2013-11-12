<#import "/WEB-INF/freemarker/permissions_macros.ftl" as pm />
<#import "/WEB-INF/freemarker/formatters.ftl" as fmt />

<#escape x as x?html>
	<div class="row-fluid">
		<div class="span6">
			<#assign popover>
				<p>A module manager can create and delete assignments, download submissions and publish feedback.</p>
			</#assign>

			<h3 class="permissionTitle">Module Managers</h3> <a class="use-popover" id="popover-modulemanager" data-html="true"
			   data-original-title="Module Managers"
			   data-content="${popover}"><i class="icon-question-sign"></i></a>

			<@pm.roleTable perms_url "modulemanager-table" scope "ModuleManagerRoleDefinition" "module managers" />
		</div>

		<div class="span6">
			<#assign popover>
				<p>A module assistant can create assignments and download submissions, but cannot delete assignments or submissions, or publish feedback.</p>
			</#assign>

			<h3 class="permissionTitle">Module Assistants</h3> <a class="use-popover" id="popover-moduleassistant" data-html="true"
			   data-original-title="Module Assistants"
			   data-content="${popover}"><i class="icon-question-sign"></i></a>

			<@pm.roleTable perms_url "moduleassistant-table" scope "ModuleAssistantRoleDefinition" "module assistants" />
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

			<@pm.roleTable perms_url "moduleauditor-table" scope "ModuleAuditorRoleDefinition" "module auditors" />
		</div>
	</div>
</#escape>