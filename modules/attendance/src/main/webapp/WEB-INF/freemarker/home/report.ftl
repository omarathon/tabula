<#include "*/attendance_variables.ftl" />
<#import "*/modal_macros.ftl" as modal />

<@modal.header>
	<h2>Review missed monitroing points</h2>
</@modal.header>

<@modal.body>
	<p>
		Send a report to the Academic Office showing all students in ${command.monitoringPointSet.route.code?upper_case} ${command.monitoringPointSet.route.name} who have missed monitroing points.
	</p>
	<p>
		Once you have sent this report you will no longer be able to edit attendance at any monitoring points that have already taken place.
	</p>
	<p>
		<button class="btn review">Review report</button>
	</p>
	<div class="monitroing-points-report-preview">
		<p>The following missed monitroing points will be recorded in SITS:</p>
		<table class="table table-condensed table-bordered table-striped">
			<thead>
				<tr>
					<th>First name</th>
					<th>Last name</th>
					<th>Missed points</th>
				</tr>
			</thead>
			<tbody>
				<#list command.missedPointCount as pair>
					<tr>
						<td>${pair._1().firstName}</td>
						<td>${pair._1().lastName}</td>
						<td>${pair._2()}</td>
					</tr>
				</#list>
			</tbody>
		</table>
	</div>
	<script>
		jQuery(function($){
			$('button.review').on('click', function(){
				$('.monitroing-points-report-preview').show();
				$(this).hide();
			});
			$('.monitroing-points-report-preview').hide();
		});
	</script>
</@modal.body>

<@modal.footer>
	<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Sending&hellip;">
		Send report
	</button>
	<button class="btn" data-dismiss="modal" aria-hidden="true">Cancel</button>
</@modal.footer>