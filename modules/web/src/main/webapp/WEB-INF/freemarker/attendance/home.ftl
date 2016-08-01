<#escape x as x?html>

<div class="btn-toolbar dept-toolbar">
	<div class="btn-group dept-settings">
		<a class="btn btn-medium dropdown-toggle" data-toggle="dropdown" href="#">
			<i class="icon-calendar"></i>
			${academicYear.label}
			<span class="caret"></span>
		</a>
		<ul class="dropdown-menu pull-right">
			<li><a href="<@routes.attendance.home '2013' />"><#if academicYear.startYear == 2013><strong>13/14</strong><#else>13/14</#if></a></li>
			<#if features.academicYear2014>
				<li><a href="<@routes.attendance.home '2014' />"><#if academicYear.startYear == 2014><strong>14/15</strong><#else>14/15</#if></a></li>
			</#if>
			<#if features.academicYear2015>
				<li><a href="<@routes.attendance.home '2015' />"><#if academicYear.startYear == 2015><strong>15/16</strong><#else>15/16</#if></a></li>
			</#if>
			<#if features.academicYear2016>
				<li><a href="<@routes.attendance.home '2016' />"><#if academicYear.startYear == 2016><strong>16/17</strong><#else>16/17</#if></a></li>
			</#if>
		</ul>
	</div>
</div>

<#if user.loggedIn && user.firstName??>
	<h1 class="with-settings">Hello, ${user.firstName}</h1>
<#else>
	<h1 class="with-settings">Hello</h1>
</#if>

<p class="lead muted">
	This is a service for viewing and managing attendance monitoring points.
</p>

<#if hasProfile>
	<h2>
		<a href="<@routes.attendance.profileHome />">My attendance profile</a>
	</h2>
</#if>

<#if hasAnyRelationships>
	<h2>My students</h2>

	<ul>
		<#list relationshipTypesMap?keys as relationshipType>
			<#if relationshipTypesMapById[relationshipType.id]>
				<li><h3><a id="relationship-${relationshipType.urlPart}-${academicYear.startYear?c}" href="<@routes.attendance.agentHomeForYear relationshipType academicYear.startYear?c />">${relationshipType.studentRole?cap_first}s ${academicYear.label}</a></h3></li>
			</#if>
		</#list>
	</ul>
</#if>

<#assign can_record = (viewPermissions?size > 0) />
<#assign can_manage = (managePermissions?size > 0) />

<#if can_record || can_manage>
	<#if can_record>
		<h2>View and record monitoring points</h2>
		<ul class="unstyled">
			<#list viewPermissions as department>
				<li>
					<h3><a id="view-department-${department.code}-${academicYear.startYear?c}" href="<@routes.attendance.viewHomeForYear department academicYear.startYear?c />">${department.name} ${academicYear.label}</a></h3>
				</li>
			</#list>
		</ul>
	</#if>

	<#if can_manage>
		<h2>Create and edit monitoring schemes</h2>
		<ul class="unstyled">
			<#list managePermissions as department>
				<li>
					<h3><a id="manage-department-${department.code}-${academicYear.startYear?c}" href="<@routes.attendance.manageHomeForYear department academicYear.startYear?c/>">${department.name} ${academicYear.label}</a></h3>
				</li>
			</#list>
		</ul>
		<#if showIntro("no-route-departments-intro", "anywhere")>
			<#assign introText>
			<p>
				You can now view or manage monitoring points for any students who are enrolled in your department,
				but whose route is owned by a different department.
				This means that you will no longer see separate links to monitor students in departments
				where you have been granted permissions on a specific route.
			</p>
			</#assign>
			<a href="#"
			   id="no-route-departments-intro"
			   class="use-introductory<#if showIntro("no-route-departments-intro", "anywhere")> auto</#if>"
			   data-hash="${introHash("no-route-departments-intro", "anywhere")}"
			   data-placement="bottom"
			   data-html="true"
			   data-content="${introText}"><i class="icon-question-sign"></i></a>
		</#if>
	</#if>
<#else>
	<#if user.staff && !hasAnyRelationships>
		<p>
			You do not currently have permission to view or manage any monitoring points. Please contact your
			departmental access manager for Tabula, or email <a id="email-support-link" href="mailto:tabula@warwick.ac.uk">tabula@warwick.ac.uk</a>.
		</p>

		<script type="text/javascript">
			jQuery(function($) {
				$('#email-support-link').on('click', function(e) {
					e.stopPropagation();
					e.preventDefault();
					$('#app-feedback-link').click();
				});
			});
		</script>
	</#if>
</#if>

</#escape>