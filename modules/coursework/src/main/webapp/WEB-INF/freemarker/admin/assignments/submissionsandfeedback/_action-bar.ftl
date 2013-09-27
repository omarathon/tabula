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
								<a href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/turnitin' />"><i class="icon-book"></i> Check for plagiarism</a>
							</li>
						</#if>
						<li class="must-have-selected">
							<a class="use-tooltip" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissionsandfeedback/mark-plagiarised' />" id="mark-plagiarised-selected-button" title="Toggle whether the selected students' submissions are possibly plagiarised." data-container="body"><i class="icon-exclamation-sign"></i> Mark plagiarised</a>
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
							<li class="must-have-selected">
								<a class="use-tooltip form-post" data-container="body" 
								   title="Release the submissions for marking. First markers will be able to download their submissions from the app."
								   href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissionsandfeedback/release-submissions' />"
								   id="release-submissions-button"><i class="icon-inbox"></i> Release for marking</a>
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
						<a href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/marks" />"><i class="icon-check"></i> Add marks</a>
					</li>
					<li class="divider"></li>
					<li>
						<a href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/feedback/online" />"><i class="icon-edit"></i> Online feedback</a>
					</li>
					<li>
						<a href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/feedback/batch" />"><i class="icon-upload"></i> Upload feedback</a>
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
						<a class="form-post" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissionsandfeedback/delete' />"><i class="icon-remove"></i> Delete feedback</a>
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