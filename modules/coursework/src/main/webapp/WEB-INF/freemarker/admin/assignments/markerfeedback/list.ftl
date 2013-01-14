<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
	<h1>Feedback for ${assignment.name}</h1>
	<p>You are the <#if isFirstMarker>first marker<#else>second marker</#if> for the following submissions</p>
	<div class="btn-toolbar">
		<a class="btn" href="<@routes.downloadmarkersubmissions assignment=assignment />">
			<i class="icon-download"></i> Download submissions (${items?size})
		</a>
		<!--if features.markerFeedback-->
			<a class="btn" href="<@routes.uploadmarkerfeedback assignment=assignment />">
				<i class="icon-upload"></i> Upload feedback
			</a>
			<a class="btn" href="<@routes.markeraddmarks assignment=assignment />">
				<i class="icon-plus"></i> Add Marks
			</a>
			<div class="btn-group">
				<a id="modify-selected" class="btn dropdown-toggle" data-toggle="dropdown" href="#">
					Update selected
					<span class="caret"></span>
				</a>
				<ul class="dropdown-menu">
					<li>
						<a class="use-tooltip form-post"
						   title="Finalise marks and feedback. Changes cannot be made to marks or feedback files after this point."
						   href="<@url page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/marking-completed' />"
						   id="marking-complete-button">
							<i class="icon-ok"></i> Marking completed
						</a>
					</li>
				</ul>
			</div>

			<!-- TODO-RITCHIE move this to scripts file -->
			<script>
				$('#marking-complete-button').click(function(event){
					event.preventDefault();
					var $checkedBoxes = $(".collection-checkbox:checked", $container);
					if ($container.data('checked') != 'none') {
						var $form = $('<form></form>').attr({method:'POST',action:this.href}).hide();
						$form.append($checkedBoxes.clone());
						$(document.body).append($form);
						$form.submit();
					}
					return false;
				});
			</script>
	    <!--/if-->
	</div>

	<div class="submission-feedback-list">
		<div class="clearfix">
			<@form.selector_check_all />
		</div>
		<table class="table table-bordered table-striped">
			<tr>
				<th></th>
				<th>Student</th>
				<th>Date submitted</th>
				<th>Mark</th>
				<th>Feedback files</th>
				<th>Status</th>
			</tr>
			<#list items as item>
				<tr>
					<td><@form.selector_check_row "students" item.student.warwickId /></td>
					<td>
						<#if assignment.module.department.showStudentName>
							${item.student.fullName}
						<#else>
							${item.student.warwickId}
						</#if>
					</td>
					<td><@fmt.date date=item.submission.submittedDate seconds=true capitalise=true /></td>
					<td><#if item.markerFeedback??>
						${item.markerFeedback.mark!''}
					</#if></td>
					<td><#if item.markerFeedback??>
						<#assign attachments=item.markerFeedback.attachments />
						<#if attachments?size gt 0>
							<a class="btn long-running" href="<@url page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/feedback/download/${item.markerFeedback.id}/feedback-${item.markerFeedback.feedback.universityId}.zip'/>">
								<i class="icon-download"></i>
								${attachments?size}
								<#if attachments?size == 1> file<#else> files</#if>
							</a>
						</#if>
					</#if></td>
					<td>
						<#if item.markerFeedback.state.toString == "ReleasedForMarking">
							<span class="label-orange">Ready for marking</span>
						<#elseif item.markerFeedback.state.toString == "DownloadedByMarker">
							<span class="label-blue">Downloaded</span>
						<#elseif item.markerFeedback.state.toString == "MarkingCompleted">
							<span class="label-green">Marking completed</span>
						</#if>
					</td>
				</tr>
			</#list>
		</table>
	</div>
</#escape>




