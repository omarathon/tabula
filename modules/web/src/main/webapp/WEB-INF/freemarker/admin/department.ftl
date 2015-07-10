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
				<i class="icon-wrench fa fa-wrench"></i>
				Manage
				<span class="caret"></span>
			</a>
			<ul class="dropdown-menu pull-right">
				<li><a href="<@routes.admin.deptperms department/>">
					<i class="icon-user fa fa-user icon-fixed-width fa fa-fw"></i> Edit departmental permissions
				</a></li>

				<li class="divider"></li>

				<#if modules?has_content || departmentRoutes?has_content || !department.children?has_content>
					<li><a href="<@routes.admin.displaysettings department />?returnTo=${(info.requestedUri!"")?url}">
						<i class="icon-list-alt fa fa-list-alt icon-fixed-width fa fa-fw"></i> Display settings</a>
					</li>
					<li><a href="<@routes.admin.notificationsettings department />?returnTo=${(info.requestedUri!"")?url}">
						<i class="icon-envelope fa fa-envelope icon-fixed-width fa fa-fw"></i> Notification settings</a>
					</li>

					<li class="divider"></li>
				</#if>

				<#if can.do("Department.Manage", department)>
					<li><a href="<@routes.admin.createsubdepartment department />">
						<i class="icon-plus fa fa-plus icon-fixed-width fa fa-fw"></i> Create sub-department</a>
					</li>
				</#if>
				<#if can.do("Department.Manage", department)>
					<li><a href="<@routes.admin.editdepartment department />">
						<i class="icon-plus fa fa-plus icon-fixed-width fa fa-fw"></i> Edit department</a>
					</li>
				</#if>
				<#if can.do("Module.Create", department)>
					<li><a href="<@routes.admin.createmodule department />">
						<i class="icon-plus fa fa-plus icon-fixed-width fa fa-fw"></i> Create module</a>
					</li>
				</#if>
				<#if department.children?has_content && can.do("Department.ArrangeRoutesAndModules", department)>
					<li class="divider"></li>

					<li><a href="<@routes.admin.sortmodules department />">
						<i class="icon-random fa fa-random icon-fixed-width fa fa-fw"></i> Arrange modules</a>
					</li>
				</#if>
				<#if department.children?has_content && can.do("Department.ArrangeRoutesAndModules", department)>
					<li><a href="<@routes.admin.sortroutes department />">
						<i class="icon-random fa fa-random icon-fixed-width fa fa-fw"></i> Arrange routes</a>
					</li>
				</#if>
			</ul>
		</div>
	</div>

	<@fmt.deptheader "" "" department routes.admin "departmenthome" "with-settings" />

<div class="row-fluid">
	<div class="span6">
		<h3>Modules</h3>

		<#if !modules?has_content>
			<#if department.children?has_content>
				<p class="alert alert-info"><i class="icon-info-sign fa fa-info-circle"></i> This department doesn't directly contain any modules. Check subdepartments.</p>
			<#else>
				<p class="alert alert-info"><i class="icon-info-sign fa fa-info-circle"></i> This department doesn't contain any modules.</p>
			</#if>
		</#if>

		<#list modules as module>
			<#assign can_manage=can.do("RolesAndPermissions.Create", module) />

			<a id="${module_anchor(module)}"></a>
			<div class="striped-section" data-name="${module_anchor(module)}">
				<div class="clearfix">
					<div class="btn-group section-manage-button">
						<#if can_manage>
							<a class="btn btn-medium dropdown-toggle" data-toggle="dropdown"><i class="icon-wrench fa fa-wrench"></i> Manage <span class="caret"></span></a>
							<ul class="dropdown-menu pull-right">
								<li><a href="<@routes.admin.moduleperms module />">
									<i class="icon-user fa fa-user"></i> Edit module permissions
								</a></li>
					  		</ul>
						<#else>
							<a class="btn btn-medium dropdown-toggle disabled" title="You do not have permission to manage this module"><i class="icon-wrench fa fa-wrench"></i> Manage <span class="caret"></span></a>
						</#if>
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
				<p class="alert alert-info"><i class="icon-info-sign fa fa-info-circle"></i> This department doesn't directly contain any routes. Check subdepartments.</p>
			<#else>
				<p class="alert alert-info"><i class="icon-info-sign fa fa-info-circle"></i> This department doesn't contain any routes.</p>
			</#if>
		</#if>

		<#list departmentRoutes as route>
			<#assign can_manage=can.do("RolesAndPermissions.Create", route) />

			<a id="${route_anchor(route)}"></a>
			<div class="striped-section" data-name="${route_anchor(route)}">
				<div class="clearfix">
					<div class="btn-group section-manage-button">
						<#if can_manage>
							<a class="btn btn-medium dropdown-toggle" data-toggle="dropdown"><i class="icon-wrench fa fa-wrench"></i> Manage <span class="caret"></span></a>
							<ul class="dropdown-menu pull-right">
								<li><a href="<@routes.admin.routeperms route />">
									<i class="icon-user fa fa-user"></i> Edit route permissions
								</a></li>
							</ul>
						<#else>
							<a class="btn btn-medium dropdown-toggle disabled" title="You do not have permission to manage this route"><i class="icon-wrench fa fa-wrench"></i> Manage <span class="caret"></span></a>
						</#if>
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
