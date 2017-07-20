<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>

<#macro link_to_department department>
	<a href="<@routes.cm2.departmenthome department academicYear />">
		${department.name}
	</a>
</#macro>

<#if is_admin && !is_marker>
	<h1>Administration</h1>
</#if>

<div class="row">
	<div class="col-md-6">
		<h2>Late and unusual activity</h2>

		<#import "*/activity_macros.ftl" as activity />
		<div class="home-page-activity">
			<@activity.activity_stream max=5 minPriority=0.5 types="SubmissionReceived,MarkedPlagiarised,Cm2MarkedPlagiarised"/>
		</div>
	</div>

	<#if is_admin>
		<div class="col-md-6">
			<#if nonempty(adminInformation.moduleManagerDepartments)>
				<h2>My managed modules</h2>

				<ul>
					<#list adminInformation.moduleManagerDepartments as department>
						<li><@link_to_department department /></li>
					</#list>
				</ul>
			</#if>

			<#if nonempty(adminInformation.adminDepartments)>
				<h2>My department-wide responsibilities</h2>

				<ul>
					<#list adminInformation.adminDepartments as department>
						<li><@link_to_department department /></li>
					</#list>
				</ul>
			</#if>
		</div>
	</#if>
</div>

</#escape>