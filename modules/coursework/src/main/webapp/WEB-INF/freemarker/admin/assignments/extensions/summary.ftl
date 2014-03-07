<#assign module = assignment.module />
<#assign department = module.department />
<#assign time_remaining=durationFormatter(assignment.closeDate) />

<#import "../submissionsandfeedback/_submission_details.ftl" as sd />

<#macro row graph>
	<tr class="itemContainer" data-contentid="${graph.universityId}">
		<#if department.showStudentName>
			<td class="student-col toggle-cell"><h6 class="toggle-icon">${graph.user.firstName}</h6></td>
			<td class="student-col toggle-cell"><h6>${graph.user.lastName}</h6></td>
		<#else>
			<td class="student-col toggle-cell"><h6 class="toggle-icon">${graph.universityId}</h6></td>
		</#if>
		<td class="status-col toggle-cell content-cell">
			<dl style="margin: 0; border-bottom: 0;">
				<dt>
					<#if graph.hasOutstandingExtensionRequest>
						<span class="label label-warning">Awaiting review</span>
					<#elseif graph.hasApprovedExtension>
						<span class="label label-info">Approved</span>
					<#elseif graph.hasRejectedExtension>
						<span class="label label-important">Rejected</span>
					<#else>
						<span class="label">No extension</span>
					</#if>
				</dt>
				<dd style="display: none;" class="table-content-container" data-contentid="${graph.universityId}">
					<div id="content-${graph.universityId}" class="content-container" data-contentid="${graph.universityId}">
						<p>No extension data is currently available.</p>
					</div>
				</dd>
			</dl>
		</td>
	</tr>
</#macro>

<#escape x as x?html>
	<h1>Manage extensions</h1>
	<h5><span class="muted">for</span> ${assignment.name} (${assignment.module.code?upper_case})</h5>

	<div class="row-fluid extension-metadata">
		<div class="span7">

			<#if assignment.closed>
				<p class="late deadline">
					<i class="icon-calendar icon-3x pull-left"></i>
					<span class="time-remaining">Closed ${time_remaining} ago</span>
					Deadline was <@fmt.date date=assignment.closeDate />
				</p>
			<#else>
				<p class="deadline">
					<i class="icon-calendar icon-3x pull-left"></i>
					<span class="time-remaining">Closes in ${time_remaining}</span>
					Deadline <@fmt.date date=assignment.closeDate />
				</p>
			</#if>
		</div>
		<div class="span5">
			<p class="alert alert-info">
				<i class="icon-envelope-alt"></i> Students will automatically be notified by email when you grant, modify or revoke an extension.
			</p>
		</div>
	</div>

	<#if extensionGraphs?size gt 0>
		<table class="students table table-bordered table-striped tabula-orangeLight sticky-table-headers expanding-table">
			<thead>
				<tr>
					<#if department.showStudentName>
						<th class="student-col">First name</th>
						<th class="student-col">Last name</th>
					<#else>
						<th class="student-col">University ID</th>
					</#if>

					<th class="status-col">Status</th>
				</tr>
			</thead>

			<tbody>
				<#list extensionGraphs as extensionGraph>
					<#-- FIXME only show row if permitted to do the action
					 ie. if (!extension && canDo(CREATE)) || (extension && canDo(UPDATE)) -->
					<@row extensionGraph />
				</#list>
			</tbody>
		</table>

		<script type="text/javascript">
		(function($) {
			$('.expanding-table').expandingTable({
				contentUrl: '${url(detailUrl!"")}',
				useIframe: true,
				tableSorterOptions: { sortList: [<#if department.showStudentName>[2, 0], </#if>[1, 0], [0,0]] }
			});
		})(jQuery);
		</script>
	<#else>
		<p class="alert alert-info">There are no students registered for this assignment.</p>
	</#if>
</#escape>