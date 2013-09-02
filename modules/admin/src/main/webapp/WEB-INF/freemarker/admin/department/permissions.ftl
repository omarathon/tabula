<#compress><#escape x as x?html>

<#import "/WEB-INF/freemarker/permissions_macros.ftl" as pm />
<#assign deptperms_url><@routes.deptperms department/></#assign>

<div id="department-permissions-page">
	<h1>Departmental permissions</h1>
	<h5>for ${department.name}</h5>

	<@pm.alerts "addCommand" department.name users role />

	<div class="row-fluid">
		<div class="span6">
			<h3 class="permissionTitle">Senior tutors</h3> <a class="use-popover" id="popover-seniortutors" data-html="true"
			   data-original-title="Senior tutors"
			   data-content="A senior tutor can see everything that a personal tutor can, for every student in the department."><i class="icon-question-sign"></i></a>

			<@pm.roleTable deptperms_url "tutor-table" department "StudentRelationshipAgentRoleDefinition(tutor)" "senior tutors" />
		</div>

		<div class="span6">
			<h3 class="permissionTitle">Senior supervisors</h3> <a class="use-popover" id="popover-seniorsupervisors" data-html="true"
			   data-original-title="Senior supervisors"
			   data-content="A senior supervisor can see everything that a supervisor can, for every student in the department."><i class="icon-question-sign"></i></a>

			<@pm.roleTable deptperms_url "supervisor-table" department "StudentRelationshipAgentRoleDefinition(supervisor)" "senior supervisors" />
		</div>
	</div>
</div>

<@pm.script />

</#escape></#compress>