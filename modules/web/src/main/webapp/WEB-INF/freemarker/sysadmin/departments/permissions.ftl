<#compress><#escape x as x?html>

<#import "/WEB-INF/freemarker/permissions_macros.ftl" as pm />
<#assign deptperms_url><@url page="/sysadmin/departments/${department.code}/permissions" /></#assign>

<div class="permissions-page">
	<h1>Departmental permissions</h1>
	<h5><span class="muted">for</span> ${department.name}</h5>

	<@pm.alerts "addCommand" department.name users role />

	<div class="row-fluid">
		<div class="span6">
			<#assign popover>
				<p>A departmental administrator has overarching responsibility for a department and everything in it.</p>
				<p>It includes most rights except for some PDP functions, which are restricted to senior tutors and supervisors.</p>
				<p>Note also that departmental admins for sub-departments get <b>no</b> automatic rights over the parent department.</p>
			</#assign>

			<h3 class="permissionTitle">Departmental admins</h3> <a class="use-popover colour-h3" id="popover-deptadmins" data-html="true"
			   data-original-title="Departmental admins"
			   data-content="${popover}"><i class="icon-question-sign fa fa-question-circle"></i></a>

			<@pm.roleTable deptperms_url "deptadmin-table" department "DepartmentalAdministratorRoleDefinition" "departmental administrators" />
		</div>
		<div class="span6">
			<#assign popover>
				<p>A User Access Manager is the person responsible for delegating access rights over a department in Tabula.</p>
				<p>The User Access Manager role can view all data in Tabula and assign all levels of permissions to staff in the department.
				   This includes assigning the role of Senior Tutor/Supervisor. User Access Managers can assign Departmental Administrators as well as nominate other User Access Managers.</p>
			</#assign>

			<h3 class="permissionTitle">Departmental User Access Manager</h3> <a class="use-popover colour-h3" id="popover-uam" data-html="true"
			   data-original-title="Departmental User Access Manager"
			   data-content="${popover}"><i class="icon-question-sign fa fa-question-circle"></i></a>

			<@pm.roleTable deptperms_url "deptuam-table" department "UserAccessMgrRoleDefinition" "user access managers" />
		</div>

	</div>
</div>

<@pm.script />

</#escape></#compress>
