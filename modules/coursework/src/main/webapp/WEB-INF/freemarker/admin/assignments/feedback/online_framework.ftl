<#assign module = assignment.module />
<#assign department = module.department />
<#assign feedbackGraphs = studentFeedbackGraphs />
<#macro row graph>
	<#assign u = graph.student />
	<tr class="itemContainer" data-contentid="${u.warwickId}">
		<#if department.showStudentName>
			<td class="student-col toggle-cell"><h6 class="toggle-icon" >${u.firstName}</h6></td>
			<td class="student-col toggle-cell"><h6>${u.lastName}</h6></td>
		<#else>
			<td class="student-col toggle-cell"><h6 class="toggle-icon">${u.warwickId}</h6></td>
		</#if>
		<td class="status-col toggle-cell content-cell">
			<dl style="margin: 0; border-bottom: 0;">
				<dt>
					<#if graph.hasSubmission>
						<div class="label">Submitted</div>
					<#else>
						<div class="label label-warning">No submission</div>
					</#if>
					<#if graph.hasPublishedFeedback>
						<div class="label label-success">Published</div>
					<#elseif graph.hasCompletedFeedback>
						<div class="label label-success">Marking completed</div>
					<#elseif graph.hasFeedback>
						<div class="label label-warning marked">Marked</div>
					</#if>
				</dt>
				<dd style="display: none;" class="table-content-container" data-contentid="${u.warwickId}">
					<div id="content-${u.warwickId}" class="feedback-container content-container" data-contentid="${u.warwickId}">
						<p>No data is currently available.</p>
					</div>
				</dd>
			</dl>
		</td>
	</tr>
</#macro>

<#escape x as x?html>
	<h1>Online marking for ${assignment.name} (${assignment.module.code?upper_case})</h1>

	<#import "../turnitin/_report_macro.ftl" as tin />
	<#import "../submissionsandfeedback/_submission_details.ftl" as sd />

	<#-- TODO 20 day turnaround deadline status alert thing rendering -->

	<div class="generic-feedback">
		<h6 class="toggle-icon edit-generic">
			<i class="row-icon icon-chevron-right icon-fixed-width" style="margin-top: 2px;"></i>
			Generic feedback
		</h6>
		<div class="edit-generic-container" style="display: none;"></div>
	</div>

	<table id="online-marking-table" class="students table table-bordered table-striped tabula-greenLight sticky-table-headers">
		<thead<#if feedbackGraphs?size == 0> style="display: none;"</#if>>
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

		<#if feedbackGraphs?size gt 0>
			<tbody>
				<#list feedbackGraphs as graph>
					<@row graph />
				</#list>
			</tbody>
		</#if>
	</table>

	<#if feedbackGraphs?size gt 0>
		<script type="text/javascript" src="/static/libs/jquery-tablesorter/jquery.tablesorter.min.js"></script>
		<script type="text/javascript">
		(function($) {
			var tsOptions = {
				sortList: [<#if department.showStudentName>[2, 0], </#if>[1, 0], [0,0]]
			};

			$('#online-marking-table').expandingTable({
				contentUrl: '${info.requestedUri!""}',
				useIframe: true,
				tableSorterOptions: tsOptions
			});
		})(jQuery);
		</script>
	<#else>
		<p>There are no submissions to mark for this assignment.</p>
	</#if>
</#escape>