<#import "../turnitin/_report_macro.ftl" as tin />
<#import "../submissionsandfeedback/_submission_details.ftl" as sd />
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />

<#function markingId user>
	<#if !user.warwickId?has_content || user.getExtraProperty("urn:websignon:usersource")! == 'WarwickExtUsers'>
		<#return user.userId />
	<#else>
		<#return user.warwickId />
	</#if>
</#function>

<#macro row info>
	<#assign u = info.student />
	<tr data-toggle="collapse" data-target="#row-${markingId(u)}" class="clickable collapsed expandable-row">
		<td class="student-col"><h6 class="toggle-icon">${u.firstName}</h6></td>
		<td class="student-col">
			<h6>${u.lastName}&nbsp;<#if u.warwickId??><@pl.profile_link u.warwickId /><#else><@pl.profile_link u.userId /></#if></h6>
		</td>
		<td class="content-cell">
					<#if u.warwickId??>${u.warwickId}<#else>${u.userId!}</#if>
		</td>
	</tr>
	<tr id="row-${markingId(u)}" data-detailurl="<@routes.cm2.feedbackAdjustmentForm assignment markingId(u) />" class="collapse detail-row">
		<td colspan="3" class="detailrow-container"><p>No data is currently available. Please check that you are signed in.</p></td>
	</tr>

</#macro>

<#escape x as x?html>
	<#if studentInfo?size gt 0>
		<div class="pull-right">
			<a href="<@routes.cm2.feedbackBulkAdjustment assignment />" class="btn"><i class="icon-sort"></i> Adjust in bulk</a>
		</div>
	</#if>
	<h1>Feedback adjustment</h1>
	<h5><span class="muted">for</span> ${assignment.name} (${assignment.module.code?upper_case})</h5>

	<div id="profile-modal" class="modal fade profile-subset"></div>

	<#if studentInfo?size gt 0>
		<table id="feedback-adjustment" class="students table table-bordered table-striped tabula-greenLight sticky-table-headers">
			<thead>
				<tr>
					<th class="student-col">First name</th>
					<th class="student-col">Last name</th>
					<th class="student-col">University ID</th>
				</tr>
			</thead>
			<tbody>
				<#list studentInfo as info>
					<@row info />
				</#list>
			</tbody>
		</table>

		<script type="text/javascript">
			(function($) {
				var tsOptions = {
					sortList: [[2, 0], [1,0]],
					headers: { 0: { sorter: false} }
				};

				$('form').find('select[name=reason]>option:eq(0)').attr('selected', true);
				$('document').find('div.late-penalty').addClass("hide");

				var beforeSubmit = function($form){
					var $select = $form.find('select[name=reason]>option:selected');
					if ($select.text === "Other") {
						$select.prop("disabled", true); // disable the dropdown so only one value is passed through for the reason
					}
					return true;
				};

				var callback = function($row){
					var $form = $row.find('form');
					var $otherInput = $form.find('.other-input');
					if($otherInput.val() !== "") {
						alert("other input = " + $otherInput);
						$form.find('select[name=reason]>option:eq(3)').attr('selected', true);
						$otherInput.removeAttr("disabled");
						$otherInput.removeClass("hide");
					}else{
						if(daysLate > 0){
							$form.find('select[name=reason]>option:eq(1)').attr('selected', true);
						}
					}
					$row.trigger('tabula.formLoaded');
				};

				$('body').on('tabula.formLoaded', function(e) {
					var $row = $(e.target);
					$row.tabulaAjaxForm(beforeSubmit, callback);
				});

			})(jQuery);
		</script>

	<#else>
		<p>There are no items of feedback that can be adjusted.</p>
	</#if>

	<#if noFeedbackStudentInfo?size gt 0>
		<p><@fmt.p noFeedbackStudentInfo?size "student does" "students do" /> not have feedback you can adjust. You can only adjust feedback once marking is completed.</p>

		<table class="students table table-bordered table-striped tabula-greenLight">
			<thead>
			<tr>
				<th class="student-col">First name</th>
				<th class="student-col">Last name</th>
				<th class="student-col">University ID</th>
			</tr>
			</thead>
			<tbody>
				<#list noFeedbackStudentInfo as info>
					<tr>
						<td class="student-col"><h6>${info.student.firstName}</h6></td>
						<td class="student-col">
							<h6>${info.student.lastName}&nbsp;<#if info.student.warwickId??><@pl.profile_link info.student.warwickId /><#else><@pl.profile_link info.student.userId /></#if></h6>
						</td>
						<td>
							<#if info.student.warwickId??>${info.student.warwickId}<#else>${info.student.userId!}</#if>
						</td>
					</tr>
				</#list>
			</tbody>
		</table>
	</#if>

	<p><a class="btn" href="<@routes.cm2.assignmentsubmissionsandfeedback assignment />"><i class="icon-reply"></i> Return to previous page</a></p>
</#escape>
