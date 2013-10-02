<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>

<#function module_anchor module>
	<#return "module-${module.code}" />
</#function>

<#if department??>
	<#assign can_manage_dept=can.do("RolesAndPermissions.Create", department) />

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
					<li><a href="<@routes.deptperms department/>">
						<i class="icon-user"></i> Edit departmental permissions
					</a></li>
					<li><a href="<@routes.displaysettings department />?returnTo=${(info.requestedUri!"")?url}">
						<i class="icon-list-alt"></i> Settings</a>
					</li>
				</ul>
			</div>
		</#if>
	</div>
	
<ul class="unstyled">
	<li><h2><a href="<@url page="/admin/department/${department.code}" context="/coursework" />">Coursework Management</a></h2></li>
		
	<#if features.smallGroupTeaching>
		<li><h2><a href="<@url page="/admin/department/${department.code}" context="/groups" />" />Small Group Teaching</h2></li>
	</#if>
	
	<li><h2><a href="<@url page="/" context="/profiles" />">Student Profiles</a></h2></li>
</ul>

<#if !modules?has_content && department.children?has_content>
<p>This department doesn't directly contain any modules. Check subdepartments.</p>
</#if>

<#list modules as module>
	<#assign can_manage=can.do("RolesAndPermissions.Create", module) />

<a id="${module_anchor(module)}"></a>
<div class="striped-section" data-name="${module_anchor(module)}">
	<div class="clearfix">
		<div class="btn-group section-manage-button">
		  <a class="btn btn-medium dropdown-toggle" data-toggle="dropdown"><i class="icon-wrench"></i> Manage <span class="caret"></span></a>
		  <ul class="dropdown-menu pull-right">
		  	<#if can_manage>	
					<li><a href="<@routes.moduleperms module />">
						<i class="icon-user"></i> Edit module permissions
					</a></li>
				</#if>
		  </ul>
		</div>
		
		<h2 class="section-title with-button"><@fmt.module_name module /></h2>
	</div>	
</div>
</#list>
<#else>
<p>No department.</p>
</#if>

</#escape>
