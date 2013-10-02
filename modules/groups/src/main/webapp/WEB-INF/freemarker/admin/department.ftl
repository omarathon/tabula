<#--





This will soon be refactored to use some components from group_components.ftl,
in the same way that tutor_home.ftl and TutorHomeController are currently

If you are doing any work on this, it would be good to do the above first.



-->
<#import "../group_components.ftl" as components />
<#escape x as x?html>

<#macro longDateRange start end>
	<#local openTZ><@warwick.formatDate value=start pattern="z" /></#local>
	<#local closeTZ><@warwick.formatDate value=end pattern="z" /></#local>
	<@fmt.date start /> 
	<#if openTZ != closeTZ>(${openTZ})</#if>
	-<br>
	<@fmt.date end /> (${closeTZ})
</#macro>

<#function module_anchor module>
	<#return "module-${module.code}" />
</#function>

<#if department??>
	<#assign can_manage_dept=data.canManageDepartment />

	<h1 class="with-settings">
		${department.name}
	</h1>
	
	<div class="btn-toolbar dept-toolbar">
	
		<#if department.parent??>
			<a class="btn btn-medium use-tooltip" href="<@routes.departmenthome department.parent />" data-container="body" title="${department.parent.name}">
				Parent department
			</a>
		</#if>

		<#if department.children?has_content>
			<div class="btn-group">
				<a class="btn btn-medium dropdown-toggle" data-toggle="dropdown" href="#">
					Subdepartments
					<span class="caret"></span>
				</a>
				<ul class="dropdown-menu pull-right">
					<#list department.children as child>
						<li><a href="<@routes.departmenthome child />">${child.name}</a></li>
					</#list>
				</ul>
			</div>
		</#if>

		<#if !modules?has_content && department.children?has_content>
			<a class="btn btn-medium dropdown-toggle disabled use-tooltip" title="This department doesn't directly contain any modules. Check subdepartments.">
				<i class="icon-wrench"></i>
				Manage
			</a>
		<#else>
			<div class="btn-group dept-settings">
				<a class="btn btn-medium dropdown-toggle" data-toggle="dropdown" href="#">
					<i class="icon-wrench"></i>
					Manage
					<span class="caret"></span>
				</a>
				<ul class="dropdown-menu pull-right">
					<li><a href="<@routes.displaysettings department />?returnTo=${(info.requestedUri!"")?url}"><i class="icon-list-alt icon-fixed-width"></i> Display settings</a></li>
					<#if features.smallGroupTeachingStudentSignUp>
					<li ${data.hasOpenableGroupsets?string(''," class='disabled use-tooltip' title='There are no self-signup groups to open' ")} >
						<a href="<@routes.batchopen department />"><i class="icon-unlock-alt icon-fixed-width"></i> Open</a></li>
					<li ${data.hasCloseableGroupsets?string(''," class='disabled use-tooltip' title='There are no self-signup groups to close' ")} >
						<a href="<@routes.batchclose department />"><i class="icon-lock icon-fixed-width"></i> Close</a></li>
					 </#if>
					<li ${data.hasUnreleasedGroupsets?string(''," class='disabled use-tooltip' title='All modules already notified' ")} >
						<a href="<@routes.batchnotify department />"><i class="icon-envelope-alt icon-fixed-width"></i> Notify</a></li>

				</ul>
			</div>


			<div class="btn-group dept-show">
				<a class="btn btn-medium use-tooltip" href="#" data-container="body" title="Modules with no groups are hidden. Click to show all modules." data-title-show="Modules with no groups are hidden. Click to show all modules." data-title-hide="Modules with no groups are shown. Click to hide them">
					<i class="icon-eye-open"></i>
					Show
				</a>
			</div>

		</#if>
	</div>

<#if !data.moduleItems?has_content && department.children?has_content>
<p>This department doesn't directly contain any modules. Check subdepartments.</p>
</#if>

<#-- This is the big list of modules -->
<@components.module_info data />

<div id="modal-container" class="modal fade"></div>
<#else>
<p>No department.</p>
</#if>

</#escape>
