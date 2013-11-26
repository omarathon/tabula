<#if students??>
	<div class="btn-toolbar">
		<div class="pull-right view-selector">
			<form class="form-inline">
				<label class="radio">View as:</label>
				<label class="radio">
					<input type="radio" name="view" value="summary" data-href="<@routes.assignmentsubmissionsandfeedbacksummary assignment />" <#if currentView == 'summary'>checked="checked"</#if> />
					Summary
				</label>
				<label class="radio">
					<input type="radio" name="view" value="table" data-href="<@routes.assignmentsubmissionsandfeedbacktable assignment />" <#if currentView == 'table'>checked="checked"</#if> />
					Table
				</label>
			</form>
		</div>

		<script type="text/javascript">
			jQuery(function($) {
				$('.view-selector input[name="view"]').on('change', function() {
					var $this = $(this);

					if ($this.is(':checked')) {
						var $form = $('<form></form>').attr({method:'POST',action:$this.data('href')}).hide();

        		var $inputs = $(':input', '.filter-form');
        		$form.append($inputs.clone());

            $(document.body).append($form);
            $form.submit();
					}
				});
			});
		</script>

		<div class="btn-group-group">
			<div class="btn-group">
				<a class="btn hover"><i class="icon-cog"></i> Actions:</a>
			</div>

			<#if assignment.collectSubmissions>
				<div class="btn-group">
					<a class="btn dropdown-toggle" data-toggle="dropdown">
						Submission
						<span class="caret"></span>
					</a>
					<ul class="dropdown-menu">
						<li class="must-have-selected">
							<a class="long-running use-tooltip form-post"
								 href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissions.zip'/>"
								 title="Download the submission files for the selected students as a ZIP file."
								 data-container="body"><i class="icon-download"></i> Download submission
							</a>
						</li>
						<li class="must-have-selected">
							<#assign deletesubmissionurl><@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissionsandfeedback/delete' /></#assign>
							<@fmt.permission_button permission='Submission.Delete' scope=module action_descr='delete submission' classes="form-post" href=deletesubmissionurl tooltip='Delete submission' >
								<i class="icon-remove"></i> Delete submission
							</@fmt.permission_button>
						</li>
					</ul>
				</div>
			<#else>
				<div class="btn-group">
					<a class="btn dropdown-toggle disabled use-tooltip" title="This assignment does not collect submissions" data-container="body">
						Submission
						<span class="caret"></span>
					</a>
				</div>
			</#if>

			<#if department.plagiarismDetectionEnabled && assignment.collectSubmissions>
				<div class="btn-group">
					<a class="btn dropdown-toggle" data-toggle="dropdown">
						Plagiarism
						<span class="caret"></span>
					</a>
					<ul class="dropdown-menu">
						<#if features.turnitin>
							<li>
								<#assign checkplagiarism_url><@url page='/admin/module/${module.code}/assignments/${assignment.id}/turnitin' /></#assign>
								<@fmt.permission_button permission='Submission.CheckForPlagiarism' scope=assignment action_descr='check for plagiarism' href=checkplagiarism_url tooltip='Check for plagiarism'>
									<i class="icon-book"></i> Check for plagiarism
								</@fmt.permission_button>
							</li>
						</#if>
						<li class="must-have-selected">
							<#assign markplagiarised_url><@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissionsandfeedback/mark-plagiarised' /></#assign>
							<@fmt.permission_button permission='Submission.ManagePlagiarismStatus' scope=assignment action_descr='mark plagiarised' href=markplagiarised_url id="mark-plagiarised-selected-button" tooltip="Toggle whether the selected student submissions are possibly plagiarised" data_attr='data-container=body'>
								<i class="icon-exclamation-sign"></i> Mark plagiarised
							</@fmt.permission_button>
						</li>
					</ul>
				</div>
			<#elseif assignment.collectSubmissions>
				<div class="btn-group">
					<a class="btn dropdown-toggle disabled use-tooltip" title="Your department does not use plagiarism detection in Tabula" data-container="body">
						Plagiarism
						<span class="caret"></span>
					</a>
				</div>
			<#else>
				<div class="btn-group">
					<a class="btn dropdown-toggle disabled use-tooltip" title="This assignment does not collect submissions" data-container="body">
						Plagiarism
						<span class="caret"></span>
					</a>
				</div>
			</#if>
			<#if assignment.collectSubmissions && features.markingWorkflows>
				<#if mustReleaseForMarking?default(false)>
					<div class="btn-group">
						<a class="btn dropdown-toggle" data-toggle="dropdown">
							Marking
							<span class="caret"></span>
						</a>
						<ul class="dropdown-menu">
							<#if assignment.markingWorkflow?? && !assignment.markingWorkflow.studentsChooseMarker>
								<li>
									<#assign markers_url><@routes.assignMarkers assignment /></#assign>
									<@fmt.permission_button
										permission='Assignment.Update'
										scope=assignment
										action_descr='assign markers'
										href=markers_url>
			            	<i class="icon-user"></i> Assign markers
			            </@fmt.permission_button>
								</li>
							<#else>
								<li class="disabled"><a><i class="icon-user"></i> Assign markers </a></li>
							</#if>
							<li class="must-have-selected">
								<#assign releaseForMarking_url><@routes.releaseForMarking assignment /></#assign>
								<@fmt.permission_button
									permission='Submission.ReleaseForMarking'
									scope=assignment
									action_descr='release for marking'
									classes='form-post'
									href=releaseForMarking_url
									id="release-submissions-button"
									tooltip="Release the submissions for marking. First markers will be able to download their submissions from the app."
									data_attr='data-container=body'>
									<i class="icon-inbox"></i> Release for marking
								</@fmt.permission_button>
							</li>
						</ul>
					</div>
				<#else>
					<div class="btn-group">
						<a class="btn dropdown-toggle disabled use-tooltip" title="This assignment does not use a marking workflow that requires assignments to be released for marking" data-container="body">
							Marking
							<span class="caret"></span>
						</a>
					</div>
				</#if>
			</#if>

			<div class="btn-group">
				<a class="btn dropdown-toggle" data-toggle="dropdown">
					Feedback
					<span class="caret"></span>
				</a>
				<ul class="dropdown-menu">
					<#if features.feedbackTemplates && assignment.hasFeedbackTemplate>
						<li>
							<a class="long-running use-tooltip"
								 href="<@url page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback-templates.zip'/>"
								 title="Download feedback templates for all students as a ZIP file."
								 data-container="body"><i class="icon-download"></i> Download templates
							</a>
						</li>
						<li class="divider"></li>
					</#if>
					<li>
						<#assign marks_url><@routes.addMarks assignment /></#assign>
						<@fmt.permission_button
							permission='Marks.Create'
							scope=assignment
							action_descr='add marks'
							href=marks_url>
            	<i class="icon-check"></i> Add marks
            </@fmt.permission_button>
					</li>
					<li class="divider"></li>
					<li>
						<#assign onlinefeedback_url><@routes.onlinefeedback assignment /></#assign>
						<@fmt.permission_button
							permission='Feedback.Read'
							scope=assignment
							action_descr='manage online feedback'
							href=onlinefeedback_url>
            	<i class="icon-edit"></i> Online feedback
            </@fmt.permission_button>
					</li>
					<li>
						<#assign feedback_url><@routes.addFeedback assignment /></#assign>
						<@fmt.permission_button
							permission='Feedback.Create'
							scope=assignment
							action_descr='upload feedback'
							classes='feedback-link'
							href=feedback_url>
            	<i class="icon-upload"></i> Upload feedback
            </@fmt.permission_button>
					</li>
					<li class="must-have-selected">
						<a class="long-running use-tooltip form-post"
							 href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/feedbacks.zip'/>"
							 title="Download the feedback files for the selected students as a ZIP file."
							 data-container="body"><i class="icon-download"></i> Download feedback
						</a>
					</li>
					<li>
						<#assign publishfeedbackurl><@url page='/admin/module/${module.code}/assignments/${assignment.id}/publish'/></#assign>
						<@fmt.permission_button permission='Feedback.Publish' scope=module type='a' action_descr='release feedback to students' tooltip="Release feedback to students" href=publishfeedbackurl>
							<i class="icon-share"></i> Publish feedback
						</@fmt.permission_button>
					</li>
					<li class="must-have-selected">
						<#assign deletefeedback_url><@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissionsandfeedback/delete' /></#assign>
						<@fmt.permission_button permission='Feedback.Delete' scope=assignment action_descr='delete feedback' classes="form-post" href=deletefeedback_url tooltip='Delete feedback'>
							<i class="icon-remove"></i> Delete feedback
						</@fmt.permission_button>
					</li>
				</ul>
			</div>
		</div>

		<div class="btn-group">
			<a class="btn dropdown-toggle" data-toggle="dropdown">
				Save As
				<span class="caret"></span>
			</a>
			<ul class="dropdown-menu">
				<li>
					<a class="long-running form-post include-filter" title="Export submissions info as XLSX, for advanced users." href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/export.xlsx'/>">Excel</a>
				</li>
				<li>
					<a class="long-running form-post include-filter" title="Export submissions info as CSV, for advanced users." href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/export.csv'/>">Text (CSV)</a>
				</li>
				<li>
					<a class="long-running form-post include-filter" title="Export submissions info as XML, for advanced users." href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/export.xml'/>">Text (XML)</a>
				</li>
			</ul>
		</div>
	</div>
</#if>