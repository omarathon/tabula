<#assign module = assignment.module />
<#assign department = module.department />
<#assign time_remaining=durationFormatter(assignment.closeDate) />

<#import "../submissionsandfeedback/_submission_details.ftl" as sd />
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<div id="profile-modal" class="modal fade profile-subset"></div>

<#macro row graph>
	<#assign state = (graph.extension.state.description)!"None" />
	<tr class="itemContainer" data-contentid="${assignment.id}_${graph.universityId}"
		data-detailurl = <@routes.extensiondetail assignment graph.universityId />
	>

		<#-- TAB-2063 - The extension manager will need to know who is doing the asking, so we should always show names -->
		<td class="student-col toggle-cell"><h6 class="toggle-icon">${graph.user.firstName}</h6></td>
		<td class="student-col toggle-cell"><h6>${graph.user.lastName}&nbsp;<@pl.profile_link graph.universityId /></h6></td>

		<td class="status-col toggle-cell content-cell">
			<dl style="margin: 0; border-bottom: 0;">
				<dt data-duration="${graph.duration}"
					data-requested-extra-duration="${graph.requestedExtraDuration}"
					data-awaiting-review="${graph.awaitingReview?string}"
					data-approved="${graph.hasApprovedExtension?string}"
					data-rejected="${graph.hasRejectedExtension?string}">
					<#if graph.awaitingReview>
						<span class="label label-warning">Awaiting review</span>
					<#elseif graph.hasApprovedExtension>
						<span class="label label-success">Approved</span>
					<#elseif graph.hasRejectedExtension>
						<span class="label label-important">Rejected</span>
					<#else>
						<span class="label no-extension">No extension</span>
					</#if>
				</dt>
				<dd style="display: none;" class="table-content-container" data-contentid="${assignment.id}_${graph.universityId}">
					<div id="content-${assignment.id}_${graph.universityId}" class="content-container" data-contentid="${assignment.id}_${graph.universityId}">
						<p>No extension data is currently available.</p>
					</div>
				</dd>
			</dl>
		</td>
		<td class="duration-col toggle-cell">
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
					<span class="time-remaining">Closed ${time_remaining}</span>
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
		<table id="student-extension-management" class="students table table-bordered table-striped tabula-orangeLight sticky-table-headers expanding-table"
			   data-max-days="${maxDaysToDisplayAsProgressBar}"
			   data-row-to-open="${extensionToOpen!""}">
			<thead>
				<tr>
					<#-- TAB-2063 no respect for dept settings, we always want to see a name here -->
					<th class="student-col">First name</th>
					<th class="student-col">Last name</th>

					<th class="status-col">Status</th>
					<th class="duration-col">Length of extension</th>
				</tr>
			</thead>

			<tbody>
				<#list extensionGraphs as extensionGraph>
					<#if (extensionGraph.extension?has_content && can.do("Extension.Update", assignment)) || can.do("Extension.Create", assignment)>
						<#-- as this is a *management* screen, only show rows we can actually do something with -->
						<@row extensionGraph />
					</#if>
				</#list>
			</tbody>
		</table>

		<script type="text/javascript">
		(function($) {
			$('.expanding-table').expandingTable({
				contentUrlFunction: function($row) { return $row.data('detailurl'); },
				useIframe: true,
				tableSorterOptions: {
					sortList: [[1, 0], [0, 0]],
					headers: {
						3: { sorter: false }
					}
				},
				preventContentIdInUrl: true
			});
		})(jQuery);
		</script>
	<#else>
		<p class="alert alert-info">There are no students registered for this assignment.</p>
	</#if>
</#escape>