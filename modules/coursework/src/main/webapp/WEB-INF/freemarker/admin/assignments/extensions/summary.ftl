<#assign module = assignment.module />
<#assign department = module.department />
<#assign time_remaining=durationFormatter(assignment.closeDate) />

<#import "../submissionsandfeedback/_submission_details.ftl" as sd />
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<div id="profile-modal" class="modal fade profile-subset"></div>

<#macro row graph>
	<#assign state = (graph.extension.state.description)!"None" />
	<tr class="itemContainer" data-contentid="${graph.universityId}">
		<#if department.showStudentName>
			<td class="student-col toggle-cell"><h6 class="toggle-icon">${graph.user.firstName}</h6></td>
			<td class="student-col toggle-cell"><h6>${graph.user.lastName}&nbsp;<@pl.profile_link graph.universityId /></h6></td>
		<#else>
			<td class="student-col toggle-cell"><h6 class="toggle-icon">${graph.universityId}</h6></td>
		</#if>
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
				<dd style="display: none;" class="table-content-container" data-contentid="${graph.universityId}">
					<div id="content-${graph.universityId}" class="content-container" data-contentid="${graph.universityId}">
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
		<table class="students table table-bordered table-striped tabula-orangeLight sticky-table-headers expanding-table" data-max-days="${maxDaysToDisplayAsProgressBar}">
			<thead>
				<tr>
					<#if department.showStudentName>
						<th class="student-col">First name</th>
						<th class="student-col">Last name</th>
					<#else>
						<th class="student-col">University ID</th>
					</#if>

					<th class="status-col">Status</th>
					<th class="duration-col">Duration</th>
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
				contentUrl: '${url(detailUrl!"")}',
				useIframe: true,
				tableSorterOptions: {
					sortList: [<#if department.showStudentName>[2, 0], </#if>[1, 0], [0, 0]],
					headers: {
						3: { sorter: false }
					}
				}
			});
		})(jQuery);
		</script>

	<#-- FIXME migrate to coursework_admin.js, courses.less when working -->
	<style type="text/css">
		#main-content .students .duration-col {
			width: 50%;
		}
		#main-content .students .progress {
			margin-bottom: 0;
		}

		#main-content .students .progress.overTime {
			position: relative;
		}

		#main-content .students .progress.overTime:after {
			content: " ";
			width: 100%;
			height: 100%;
			position: absolute;
			z-index: 30;
			top: 0;
			left: 0;
			pointer-events: none;
			/* use 60% of the table stripe colour */
			background: -moz-linear-gradient(left, rgba(240,240,240,0) 90%, rgba(240,240,240,1) 100%);
			background: -webkit-gradient(linear, left top, right top, color-stop(90%,rgba(240,240,240,0)), color-stop(100%,rgba(240,240,240,1)));
			background: -webkit-linear-gradient(left, rgba(240,240,240,0) 90%, rgba(240,240,240,1) 100%);
			background: -o-linear-gradient(left, rgba(240,240,240,0) 90%, rgba(240,240,240,1) 100%);
			background: -ms-linear-gradient(left, rgba(240,240,240,0) 90%, rgba(240,240,240,1) 100%);
			background: linear-gradient(to right, rgba(240,240,240,0) 90%, rgba(240,240,240,1) 100%);
		}
	</style>
	<script type="text/javascript">
		(function($) {
			var maxDaysToDisplayAsProgressBar = $('table.students').data('max-days');
			var totalDuration;

			var barWidth = function(duration) {
				return 100 * duration / totalDuration;
			}
			var tooltip = function(verb, duration) {
				return verb + ' ' + duration + ' day' + (duration == 1 ? "" : "s");
			}
			var appendBar = function(verb, customClass, duration) {
				if (duration) {
					$progress.append($('<div class="bar ' + customClass + ' use-tooltip" title="' + tooltip(verb, duration) + '" style="width: ' + barWidth(duration) + '%" data-container="body"></div>'));
				}
			}

			$('table.students tbody tr').each(function() {
				availableWidth = 100;
				var $row = $(this);
				var $dt = $row.find('dt');
				<#-- ignore rows without extension -->
				if ($row.find('.no-extension').length == 0) {
					var duration = $dt.data('duration');
					var requestedExtraDuration = $dt.data('requestedExtraDuration');
					totalDuration = duration + requestedExtraDuration;

					var isOverTime = totalDuration > maxDaysToDisplayAsProgressBar;
					var progressClass = "progress";
					var progressWidth;
					if (isOverTime) {
						progressWidth = 100;
						progressClass += " overTime";
					} else {
						progressWidth = 90*totalDuration / maxDaysToDisplayAsProgressBar;
					}

					$progress = $('<div class="' + progressClass + '" style="width: ' + progressWidth + '%"></div>');
					if ($dt.data('rejected') && !$dt.data('awaitingReview')) {
						appendBar('Rejected request for', 'bar-danger', requestedExtraDuration);
					} else if ($dt.data('approved')) {
						appendBar('Approved', 'bar-success', duration);
					}
					if ($dt.data('awaitingReview')) {
						appendBar('Requested further', 'bar-warning', requestedExtraDuration);
					}

					$row.find('.duration-col').empty().append($progress);
				}
			});
			$('.bar').tooltip();

			$('#main-content').on('click', '.btn.revoke', function(e) {
				e.preventDefault();
				e.stopPropagation();
				var message = 'Revoking an extension is irreversible. Are you sure?';
				var modalHtml = "<div class='modal hide fade' id='confirmModal'>" +
						"<div class='modal-body'>" +
						"<h5>"+message+"</h5>" +
						"</div>" +
						"<div class='modal-footer'>" +
						"<a class='confirm btn'>Yes, revoke</a>" +
						"<a data-dismiss='modal' class='btn btn-primary'>No, go back</a>" +
						"</div>" +
						"</div>"
				var $modal = $(modalHtml);
				$modal.data('form', $(this).closest('form'));
				$modal.modal();
				$('a.confirm', $modal).on('click', function() {
					$modal.modal('hide');
					var $form = $modal.data('form');
					<#-- FIXME I hate literals -->
					$form.attr('action', $form.attr('action').replace('detail/', 'revoke/'));
					$form.submit();
				});
			});
		})(jQuery);
	</script>


	<#else>
		<p class="alert alert-info">There are no students registered for this assignment.</p>
	</#if>
</#escape>