<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>

<#function module_anchor module>
	<#return "module-${module.code}" />
</#function>

<#function route_anchor route>
	<#return "route-${route.code}" />
</#function>

<#if department??>
	<#assign can_manage_dept=can.do("RolesAndPermissions.Create", department) />

	<div class="btn-toolbar dept-toolbar">
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

				<#if modules?has_content || departmentRoutes?has_content || !department.children?has_content>
					<li><a href="<@routes.displaysettings department />?returnTo=${(info.requestedUri!"")?url}">
						<i class="icon-list-alt"></i> Settings</a>
					</li>
				</#if>

				<#if can.do("Department.Create", department)>
					<li><a href="<@routes.createsubdepartment department />">
						<i class="icon-plus"></i> Create sub-department</a>
					</li>
				</#if>
				<#if can.do("Module.Create", department)>
					<li><a href="<@routes.createmodule department />">
						<i class="icon-plus"></i> Create module</a>
					</li>
				</#if>
				<#if department.children?has_content && can.do("Department.ArrangeModules", department)>
					<li><a href="<@routes.sortmodules department />">
						<i class="icon-random"></i> Arrange modules</a>
					</li>
				</#if>
				<#if department.children?has_content && can.do("Department.ArrangeRoutes", department)>
					<li><a href="<@routes.sortroutes department />">
						<i class="icon-random"></i> Arrange routes</a>
					</li>
				</#if>
			</ul>
		</div>
	</div>

	<@fmt.deptheader "" "" department routes "departmenthome" "with-settings" />

<div class="row-fluid">
	<div class="span6">
		<h3>Modules</h3>

		<#if !modules?has_content>
			<#if department.children?has_content>
				<p class="alert alert-info"><i class="icon-info-sign"></i> This department doesn't directly contain any modules. Check subdepartments.</p>
			<#else>
				<p class="alert alert-info"><i class="icon-info-sign"></i> This department doesn't contain any modules.</p>
			</#if>
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
	</div>
	<div class="span6">
		<h3>Routes</h3>

		<#if !departmentRoutes?has_content>
			<#if department.children?has_content>
				<p class="alert alert-info"><i class="icon-info-sign"></i> This department doesn't directly contain any routes. Check subdepartments.</p>
			<#else>
				<p class="alert alert-info"><i class="icon-info-sign"></i> This department doesn't contain any routes.</p>
			</#if>
		</#if>

		<#list departmentRoutes as route>
			<#assign can_manage=can.do("RolesAndPermissions.Create", route) />

			<a id="${route_anchor(route)}"></a>
			<div class="striped-section" data-name="${route_anchor(route)}">
				<div class="clearfix">
					<div class="btn-group section-manage-button">
					  <a class="btn btn-medium dropdown-toggle" data-toggle="dropdown"><i class="icon-wrench"></i> Manage <span class="caret"></span></a>
					  <ul class="dropdown-menu pull-right">
					  	<#if can_manage>
								<li><a href="<@routes.routeperms route />">
									<i class="icon-user"></i> Edit route permissions
								</a></li>
							</#if>
					  </ul>
					</div>

					<h2 class="section-title with-button"><@fmt.route_name route true /></h2>
				</div>
			</div>
		</#list>
	</div>
</div>
<#else>
<p>No department.</p>
</#if>

</#escape>
