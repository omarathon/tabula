<#compress><#escape x as x?html>

<#import "/WEB-INF/freemarker/permissions_macros.ftl" as pm />
<#assign deptperms_url><@url page="/sysadmin/departments/${department.code}/permissions" /></#assign>

<div id="department-permissions-page">
	<h1>Departmental permissions</h1>
	<h5>for ${department.name}</h5>

	<@pm.alerts "addCommand" department.name users role />

	<#assign popover>
		<p>A departmental administrator has overarching responsibility for a department and everything in it.</p>
    	<p>It includes most rights except viewing content of meeting records, which is restricted to senior tutors and supervisors.</p>
    	<p>Note also that departmental admins for sub-departments get <b>no</b> automatic rights over the parent department.</p>
	</#assign>

	<div class="row-fluid">
		<div class="span6">
			<h3 class="permissionTitle">Departmental admins</h3> <a class="use-popover" id="popover-deptadmins" data-html="true"
			   data-original-title="Departmental admins"
			   data-content="${popover}"><i class="icon-question-sign"></i></a>

			<@pm.roleTable deptperms_url "deptadmin-table" department "DepartmentalAdministratorRoleDefinition" "departmental administrators" />
		</div>
	</div>
</div>

<@pm.script />

</#escape></#compress>
