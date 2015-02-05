<#import "../turnitin/_report_macro.ftl" as tin />
<#import "../submissionsandfeedback/_submission_details.ftl" as sd />
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<div id="profile-modal" class="modal fade profile-subset"></div>

<#function markingId user>
	<#if !user.warwickId?has_content || user.getExtraProperty("urn:websignon:usersource")! == 'WarwickExtUsers'>
		<#return user.userId />
	<#else>
		<#return user.warwickId />
	</#if>
</#function>

<#macro row info>
	<#assign u = info.student />
	<tr class="item-container" data-contentid="${markingId(u)}" data-markingurl="<@routes.feedbackAdjustment assignment />">
		<td class="student-col toggle-cell"><h6 class="toggle-icon">${u.firstName}</h6></td>
		<td class="student-col toggle-cell">
			<h6>${u.lastName}&nbsp;<@pl.profile_link u.warwickId! /></h6>
		</td>
		<td class="toggle-cell content-cell">
			<dl style="margin: 0; border-bottom: 0;">
				<dt>
					${u.warwickId!}
				</dt>
				<dd style="display: none;" class="table-content-container" data-contentid="${markingId(u)}">
					<div id="content-${markingId(u)}" class="content-container" data-contentid="${markingId(u)}">
						<p>No data is currently available. Please check that you are signed in.</p>
					</div>
				</dd>
			</dl>
		</td>
	</tr>
</#macro>

<#escape x as x?html>
	<h1>Feedback adjustment</h1>
	<h5><span class="muted">for</span> ${assignment.name} (${assignment.module.code?upper_case})</h5>
	<#if studentInfo?size gt 0>
		<table id="feedback-adjustment" class="students table table-bordered table-striped tabula-greenLight sticky-table-headers expanding-table">
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

				$('.expanding-table').expandingTable({
					contentUrlFunction: function($row){ return $row.data('markingurl'); },
					useIframe: true,
					tableSorterOptions: tsOptions
				});

				$('#container').on('change', 'select[name=reason]', function(e) {
					var $target = $(e.target);

					var $otherInput = $target.siblings('.other-input');
					if ($target.val() === "Other") {
						$otherInput.removeAttr("disabled");
						$otherInput.fadeIn(400);
					} else if ($otherInput.is(':visible')){
						$otherInput.fadeOut(400, function() {
							$otherInput.attr("disabled", "disabled");
						});
					}

					var $suggestedPenalty = $target.closest('form').find('.late-penalty');
					if ($target.val() === "Late submission penalty") {
						$suggestedPenalty.fadeIn(400);
					} else if ($suggestedPenalty.is(':visible')) {
						$suggestedPenalty.fadeOut(400);
					}

				});


				$('#container').on('submit', function(e) {
					var $form = $(e.target);
					var $select = $form.find('select[name=reason]');
					if ($select.val() === "Other") {
						$select.attr("disabled", "disabled");
					}
				});

				$('#container').on('tabula.expandingTable.parentRowExpanded', function(e){
					var $content = $(e.target);

					// activate any popovers
					$content.find('.use-popover').popover();

					// bind suggested mark button
					$content.find('.use-suggested-mark').on('click', function(e){
						var $target = $(this);
						var $markInput = $content.find('input[name=adjustedMark]');
						var $commentsTextarea = $content.find('textarea[name=comments]');
						var mark = $target.data('mark');
						var comment = $target.data('comment');
						$markInput.val(mark);
						$commentsTextarea.val(comment);
						e.preventDefault();
					});

					// pre-select the other dropdown when editing an existing adjustment with an "other" reason
					var $select = $content.find('select[name=reason]');
					var $otherInput = $content.find('.other-input');

					if($otherInput.val() != "" && $select.children(':selected').index() === 0) {
						$content.find('option[value=Other]').attr("selected", "selected");
						$otherInput.removeAttr("disabled");
						$otherInput.show();
					}

					// show the suggested mark button if late penalty is selected
					var $suggestedPenalty = $select.closest('form').find('.late-penalty');
					if ($select.val() === "Late submission penalty") {
						$suggestedPenalty.show();
					} else {
						$suggestedPenalty.hide();
					}
				})

			})(jQuery);
		</script>

	<#else>
		<p>There are no items of feedback that can be adjusted. You can only make adjustments to feedback that has been marked but not yet published.</p>
	</#if>
</#escape>
