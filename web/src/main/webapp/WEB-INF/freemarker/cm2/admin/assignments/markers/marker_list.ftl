<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "*/cm2_macros.ftl" as cm2 />
<#import "*/coursework_components.ftl" as components />

<#escape x as x?html>
	<@cm2.headerMenu department />
	<div class="deptheader">
		<h1>${assignment.name}</h1>
		<h4 class="with-related"><@fmt.module_name assignment.module /></h4>
	</div>

	<h2>Marking</h2>

	<#-- TODO - filters and actions -->

	<#list feedbackByStage?keys as stage>
		<#assign markingCompleted><@routes.cm2.markingCompleted assignment stage marker /></#assign>
		<div class="marking-stage">
			<#if feedbackByStage?keys?size gt 0>
				<h3>${stage.description}</h3>
				<#if stage.nextStagesDescription?has_content>
					<a class="btn btn-primary must-have-selected form-post" href="${markingCompleted}">Confirm selected and send to ${stage.nextStagesDescription?lower_case}</a>
				</#if>
			</#if>
			<#assign enhancedMarkerFeedbacks = mapGet(feedbackByStage, stage)/>
			<table class="table table-striped marking-table">
				<thead>
					<tr>
						<th class="check-col"><@bs3form.selector_check_all /></th>
						<#if assignment.anonymousMarking>
							<th class="student-col">ID</th>
						<#else>
							<th class="student-col">University ID</th>
							<th class="student-col">First name</th>
							<th class="student-col">Last name</th>
						</#if>
						<th colspan="2">Progress</th>
					</tr>
				</thead>
				<tbody>
					<#list enhancedMarkerFeedbacks as emf>
						<#assign mf = emf.markerFeedback />
						<#assign student = mf.student />
						<#assign studentId><#if assignment.anonymousMarking>${mf.feedback.anonymousId}<#else>${student.userId}</#if></#assign>
						<tr
							data-toggle="collapse"
							data-target="#${stage.name}-${studentId}"
							class="clickable collapsed expandable-row <#if mf.readyForNextStage>ready-next-stage</#if>"
						>
							<td class="check-col">
								<@bs3form.selector_check_row name="markerFeedback" value="${mf.id}" />
							</td>
							<#if assignment.anonymousMarking>
								<#assign colspan = 4>
								<td class="toggle-icon-large student-col"><span class=""></span>Student${mf.feedback.anonymousId}</td>
							<#else>
								<#assign colspan = 6>
								<td class="toggle-icon-large student-col">${mf.feedback.studentIdentifier!""}</td>
								<td class="student-col">${student.firstName}</td>
								<td class="student-col">${student.lastName}&nbsp;<#if student.warwickId??><@pl.profile_link student.warwickId /><#else><@pl.profile_link student.userId /></#if></td>
							</#if>
							<td class="progress-col">
								<@components.individual_stage_progress_bar emf.workflowStudent.stages/>
							</td>
							<td>
								<#if emf.workflowStudent.nextAction?has_content>
									<@spring.message code=emf.workflowStudent.nextAction />
								</#if>
							</td>
						</tr>
						<#assign detailUrl><@routes.cm2.markerOnlineFeedback assignment stage marker student /></#assign>
						<tr id="${stage.name}-${studentId}" data-detailurl="${detailUrl}" class="collapse detail-row">
							<td colspan="${colspan}" class="detailrow-container">
								<i class="fa fa-spinner fa-spin"></i> Loading
							</td>
						</tr>
					</#list>
				</tbody>
			</table>
			<#if feedbackByStage?keys?size gt 0>
				<#if stage.nextStagesDescription?has_content>
					<a class="btn btn-primary must-have-selected form-post" href="${markingCompleted}">Confirm selected and send to ${stage.nextStagesDescription?lower_case}</a>
				</#if>
			</#if>
		</div>
	</#list>
<script type="text/javascript">
	(function($) {

		var $body = $('body');



		// on cancel collapse the row and nuke the form
		$body.on('click', '.cancel', function(e){
			e.preventDefault();
			var $row = $(e.target).closest('.detail-row');
			$row.collapse("hide");

			$row.on('hidden.bs.collapse', function(e) {
				$row.data('loaded', false);
				$row.find('.detailrow-container').html('<i class="fa fa-spinner fa-spin"></i> Loading');
				$(this).unbind(e);
			});
		});

		// on reset fetch the form again
		$body.on('click', '.reset', function(e){
			e.preventDefault();
			var $row = $(e.target).closest('.detail-row');
			$row.data('loaded', false);
			$row.trigger('show.bs.collapse');
		});

		// remove attachment
		$body.on("click", '.remove-attachment', function(e) {
			e.preventDefault();
			var $this = $(this);
			var $form = $this.closest('form');
			var $li = $this.closest("li");
			$li.find('input, a').remove();
			$li.find('span').wrap('<del />');
			$li.find('i').css('display', 'none');
			var $ul = $li.closest('ul');

			if (!$ul.find('li').last().is('.pending-removal')) {
				var alertMarkup = '<li class="pending-removal">Files marked for removal won\'t be deleted until you <samp>Save</samp>.</li>';
				$ul.append(alertMarkup);
			}

			if($form.find('input[name=attachedFiles]').length === 0){
				var $blankInput = $('<input name="attachedFiles" type="hidden" />');
				$form.append($blankInput);
			}
		});

		// copy feedback
		$body.on('click', '.copy-feedback', function(e){
			e.preventDefault();
			var $this = $(this);
			var $prevFeedback = $this.closest('.previous-marker-feedback');
			var $comments = $prevFeedback.find('.feedback-comments');
			var $attachments = $prevFeedback.find('.feedback-attachments li');
			var $form = $('.marking-and-feedback form');
			var $newComments = $form.find('textarea');
			if ($newComments.val()){
				$newComments.val($newComments.val() + '\n\n')
			}
			$newComments.val($newComments.val() + $comments.val());
			var $newAttachments = $form.find('ul.attachments');
			$newAttachments.append($attachments.clone());
			$newAttachments.parent('.form-group.hide').removeClass('hide');
			$this.addClass('disabled').text('Feedback copied');
		});

		// checkbox helpers and form submission for bulk actions
		$('.marking-table').bigList({

			setup: function(){

				var $container = this, $outerContainer = $container.closest('.marking-stage');

				$('.form-post', $outerContainer).click(function(event){
					event.preventDefault();
					var $this = $(this);
					if(!$this.hasClass("disabled")) {
						var action = this.href;
						var $form = $('<form></form>').attr({method: 'POST', action: action}).hide();
						var doFormSubmit = false;

						if ($container.data('checked') !== 'none' || $this.closest('.must-have-selected').length === 0) {
							var $checkedBoxes = $(".collection-checkbox:checked", $container);
							$form.append($checkedBoxes.clone());
							doFormSubmit = true;
						}

						if (doFormSubmit) {
							$(document.body).append($form);
							$form.submit();
						} else {
							return false;
						}
					}
				});
			},

			onSomeChecked : function() {
				var $markingStage = this.closest('.marking-stage');
				if(this.find('.ready-next-stage input:checked').length){
					$markingStage.find('.must-have-selected').removeClass('disabled');
				} else {
					$markingStage.find('.must-have-selected').addClass('disabled');
				}
			},

			onNoneChecked : function() {
				var $markingStage = this.closest('.marking-stage');
				$markingStage.find('.must-have-selected').addClass('disabled');
			}
		});

		// prevent rows from expanding when selecting the checkbox column
		$body.on('click', '.check-col', function(e){
			e.stopPropagation()
		});

	})(jQuery);
</script>
</#escape>