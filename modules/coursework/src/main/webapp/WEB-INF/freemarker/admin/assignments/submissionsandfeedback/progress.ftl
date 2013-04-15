<#escape x as x?html>
<div class="fixed-on-scroll">
	<h1>${assignment.name} (${assignment.module.code?upper_case})</h1>
	
	<#if assignment.openEnded>
		<p class="dates">
			<@fmt.interval assignment.openDate />, never closes
			(open-ended)
			<#if !assignment.opened>
				<span class="label label-warning">Not yet open</span>
			</#if>
		</p>
	<#else>
		<p class="dates">
			<@fmt.interval assignment.openDate assignment.closeDate />
			<#if assignment.closed>
				<span class="label label-warning">Closed</span>
			</#if>
			<#if !assignment.opened>
				<span class="label label-warning">Not yet open</span>
			</#if>
								
		</p>
	</#if>
	
	<#assign module=assignment.module />
	<#assign department=module.department />
	
	<@f.form method="post" action="${url('/admin/module/${module.code}/assignments/${assignment.id}/list')}" cssClass="form-inline" commandName="submissionAndFeedbackCommand">
		<div class="filter">
			<label for="filter">Show all</label>
			&nbsp;
			<@f.select path="filter" cssClass="span4">
				<@f.options items=allFilters itemValue="name" itemLabel="description" />
			</@f.select>
			
			<#list allFilters as filter>
				<#if filter.parameters?size gt 0>
					<fieldset data-filter="${filter.name}" class="form-horizontal filter-options"<#if filter.name != submissionAndFeedbackCommand.filter.name> style="display: none;"</#if>>
						<#list filter.parameters as param>
							<@form.labelled_row "filterParameters[${param._1()}]" param._2()>
								<#if param._3() == 'datetime'>
									<@f.input path="filterParameters[${param._1()}]" cssClass="date-time-picker" />
								<#elseif param._3() == 'percentage'>
									<div class="input-append">
										<@f.input path="filterParameters[${param._1()}]" type="number" min="0" max="100" cssClass="input-small" />
										<span class="add-on">%</span>
									</div>
								<#else>
									<@f.input path="filterParameters[${param._1()}]" type=param._3() />
								</#if>
							</@form.labelled_row>
						</#list>
						
						<@form.row>
							<@form.field>
								<button class="btn btn-primary" type="submit">Filter</button>
							</@form.field>
						</@form.row>
					</fieldset>
				</#if>
			</#list>
		</div>
		
		<script type="text/javascript">
			jQuery(function($) {
				$('#filter').on('keyup change', function() {
					var $select = $(this);
					var val = $select.val();
					
					var $options = $select.closest('.filter').find('fieldset[data-filter="' + val + '"]');
					var $openOptions = $select.closest('.filter').find('.filter-options:visible');
					
					var cb = function() {
						if ($options.length) {
							$options.slideDown();
						} else {
							$select.closest('form').submit();
						}
					};
					
					if ($openOptions.length) {
						$openOptions.slideUp('fast', cb);
					} else {
						cb();
					}
				});
			});
		</script>
	</@f.form>

	<#if students??>
	
	<#macro originalityReport attachment>
	<#local r=attachment.originalityReport />
	
				<span id="tool-tip-${attachment.id}" class="similarity-${r.similarity} similarity-tooltip">${r.overlap}% similarity</span>
	      <div id="tip-content-${attachment.id}" class="hide">
					<p>${attachment.name} <img src="<@url resource="/static/images/icons/turnitin-16.png"/>"></p>
					<p class="similarity-subcategories-tooltip">
						Web: ${r.webOverlap}%<br>
						Student papers: ${r.studentOverlap}%<br>
						Publications: ${r.publicationOverlap}%
					</p>
					<p>
						<a target="turnitin-viewer" href="<@url page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/turnitin-report/${attachment.id}'/>">View full report</a>
					</p>
	      </div>
	      <script type="text/javascript">
	        jQuery(function($){
	          $("#tool-tip-${attachment.id}").popover({
	            placement: 'right',
	            html: true,
	            content: function(){return $('#tip-content-${attachment.id}').html();},
	            title: 'Turnitin report summary'
	          });
	        });
	      </script>
	
	</#macro>
	
	<div class="btn-toolbar">
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
							<a class="form-post" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissionsandfeedback/delete' />"><i class="icon-remove"></i> Delete submission</a>
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
						<a href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/publish" />"><i class="icon-share"></i> Publish feedback</a>
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
					<a title="Export submissions info as XLSX, for advanced users." href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/export.xlsx'/>">Excel</a>
				</li>
				<li>
					<a title="Export submissions info as CSV, for advanced users." href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/export.csv'/>">Text (CSV)</a>
				</li>
				<li>
					<a title="Export submissions info as XML, for advanced users." href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/export.xml'/>">Text (XML)</a>
				</li>
			</ul>
		</div>
	</div>
</div>

<#if students?size gt 0>
	<table id="coursework-progress-table" class="students table table-bordered table-striped tabula-greenLight sticky-table-headers">
		<thead>
			<tr>
				<th class="check-col" style="padding-right: 0px;"><input type="checkbox" class="collection-check-all"></th>
				<#if department.showStudentName>
					<th class="student-col">First name</th>
					<th class="student-col">Last name</th>
				<#else>
					<th class="student-col">University ID</th>
				</#if>
				
				<th class="progress-col">Progress</th>
				<th class="action-col">Next action</th>
			</tr>
		</thead>
		
		<tbody>
			<#macro action actionCode student>
				<#local studentDetails><span data-profile="${student.user.warwickId}"><#if department.showStudentName>${student.user.fullName}<#else>${student.user.warwickId}</#if></span></#local>
				<#local firstMarker="first marker" />
				<#local secondMarker="second marker" />
				
				<#if student.coursework.enhancedSubmission??>
					<#if student.coursework.enhancedSubmission?? && student.coursework.enhancedSubmission.submission?? && student.coursework.enhancedSubmission.submission.assignment??>
						<#if student.coursework.enhancedSubmission.submission.firstMarker?has_content>
							<#local firstMarker><span data-profile="${student.coursework.enhancedSubmission.submission.firstMarker.warwickId}">${student.coursework.enhancedSubmission.submission.firstMarker.fullName}</span></#local>
						</#if>
						
						<#if student.coursework.enhancedSubmission.submission.secondMarker?has_content>
							<#local secondMarker><span data-profile="${student.coursework.enhancedSubmission.submission.secondMarker.warwickId}">${student.coursework.enhancedSubmission.submission.secondMarker.fullName}</span></#local>
						</#if>
					</#if>
				</#if>
				
				<#local text><@spring.message code=actionCode /></#local>
			
				<#noescape>${(text!"")?replace("[STUDENT]", studentDetails)?replace("[FIRST_MARKER]", firstMarker)?replace("[SECOND_MARKER]", secondMarker)}</#noescape>
			</#macro>
		
			<#macro stage stage>
				<#if stage.messageCode?default("")?length gt 0>
					<div class="stage<#if !stage.completed> incomplete<#if !stage.preconditionsMet> preconditions-not-met</#if></#if><#if stage.started && !stage.completed> current</#if>">
						<#if stage.completed>
							<#if stage.health.toString == 'Good'>
								<i class="icon-ok"></i>
							<#else>
								<i class="icon-remove"></i>
							</#if>
						<#else>
							<i class="icon-blank"></i>
						</#if>
						<@spring.message code=stage.messageCode /><#nested/>
					</div>
				</#if>
			</#macro>
		
			<#macro workflow student>
				<#if student.coursework.enhancedSubmission??>
					<#local enhancedSubmission=student.coursework.enhancedSubmission>
					<#local submission=enhancedSubmission.submission>
				</#if>
				<#if student.coursework.enhancedFeedback??>
					<#local enhancedFeedback=student.coursework.enhancedFeedback>
					<#local feedback=enhancedFeedback.feedback>
				</#if>
				<#if student.coursework.enhancedExtension??>
					<#local enhancedExtension=student.coursework.enhancedExtension>
					<#local extension=enhancedExtension.extension>
				</#if>
				
				<#if submission?? && submission.submittedDate?? && (submission.late || submission.authorisedLate)>
					<#local lateness = "${durationFormatter(assignment.closeDate, submission.submittedDate)} after close" />
				<#else>
					<#local lateness = "" />
				</#if>
			
				<div class="workflow">
					<#if student.stages?keys?seq_contains('Submission')>
						<div class="stage-group clearfix">
							<h3>Submission</h3>
							
							<div class="labels">
								<#if submission??>
									<#if submission.late>
										<span class="label label-important use-tooltip" title="${lateness!''}" data-container="body">Late</span>
									<#elseif  submission.authorisedLate>
										<span class="label label-info use-tooltip" title="${lateness!''}" data-container="body">Within Extension</span>
									</#if>
								<#elseif !enhancedFeedback??>
									<#if enhancedExtension?has_content>
										<span class="label label-info">Unsubmitted</span>
										<#if extension.approved && !extension.rejected>
											<#local date>
												<@fmt.date date=extension.expiryDate capitalise=true shortMonth=true />
											</#local>
										</#if>
										<#if enhancedExtension.within>
											<span class="label label-info use-tooltip" title="${date}" data-container="body">Within Extension</span>
										<#elseif extension.rejected>
											<span class="label label-info">Extension Rejected</span>
										<#elseif !extension.approved>
											<span class="label label-info">Extension Requested</span>
										<#else>
											<span class="label label-info use-tooltip" title="${date}" data-container="body">Extension Expired</span>
										</#if>
									<#else>
										<span class="label label-info">Unsubmitted</span>
									</#if>
								</#if>
							</div>
							
							<#-- If the current action is in this section, then add the next action blowout here -->
							<#if student.nextStage?? && ['Submission','DownloadSubmission']?seq_contains(student.nextStage.toString) && student.progress.messageCode != 'workflow.Submission.unsubmitted.failedToSubmit'>
								<div class="alert pull-right">
									<strong>Next action:</strong> <@action student.nextStage.actionCode student />
								</div>
							</#if>
							
							<@stage student.stages['Submission']><#compress>
								<#if submission??>: <#compress>
									<#local attachments=submission.allAttachments />
									<#if attachments?size gt 0>
										<#if attachments?size == 1> 
											<#local filename = "${attachments[0].name}">
										<#else>
											<#local filename = "submission-${submission.universityId}.zip">
										</#if>
										<a class="long-running" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissions/download/${submission.id}/${filename}'/>"><#compress>
											${attachments?size}
											<#if attachments?size == 1> file
											<#else> files
											</#if>
										</#compress></a><#--
									--></#if><#--
									--><#if submission.submittedDate??> <#compress>
										<span class="date use-tooltip" title="${lateness!''}" data-container="body"><#compress>
											<@fmt.date date=submission.submittedDate seconds=true capitalise=true shortMonth=true />
										</#compress></span>
									</#compress></#if><#--
									--><#if assignment.wordCountField?? && submission.valuesByFieldName[assignment.defaultWordCountName]??><#compress>
										, ${submission.valuesByFieldName[assignment.defaultWordCountName]?number} words
									</#compress></#if>
								</#compress></#if>
							</#compress></@stage>
							
							<#if student.stages?keys?seq_contains('DownloadSubmission')>
								<@stage student.stages['DownloadSubmission'] />
							</#if>
						</div>
					</#if>
						
					<#if student.stages?keys?seq_contains('CheckForPlagiarism')>
						<div class="stage-group clearfix">
							<h3>Plagiarism</h3>
							
							<div class="labels">
								<#if submission?? && submission.suspectPlagiarised>
									<i class="icon-exclamation-sign use-tooltip" title="Suspected of being plagiarised" data-container="body"></i>
								</#if>
							</div>
							
							<#-- If the current action is in this section, then add the next action blowout here -->
							<#if student.nextStage?? && ['CheckForPlagiarism']?seq_contains(student.nextStage.toString)>
								<div class="alert pull-right">
									<strong>Next action:</strong> <@action student.nextStage.actionCode student />
								</div>
							</#if>
							
							<@stage student.stages['CheckForPlagiarism']><#compress>
								<#if submission??>
									<#list submission.allAttachments as attachment>
										<!-- Checking originality report for ${attachment.name} ... -->
										<#if attachment.originalityReport??>
											: <@originalityReport attachment />
										</#if>
									</#list>
								</#if>						
							</#compress></@stage>
						</div>
					</#if>
					
					<#if student.stages?keys?seq_contains('ReleaseForMarking')>
						<div class="stage-group clearfix">
							<h3>Marking</h3>
							
							<div class="labels">
								<#if submission?? && submission.assignment?? && submission.releasedForMarking>
									<span class="label label-success">Markable</span>
								</#if>
							</div>
							
							<#-- If the current action is in this section, then add the next action blowout here -->
							<#if student.nextStage?? && ['ReleaseForMarking','FirstMarking','SecondMarking']?seq_contains(student.nextStage.toString)>
								<div class="alert pull-right">
									<strong>Next action:</strong> <@action student.nextStage.actionCode student />
								</div>
							</#if>
							
							<@stage student.stages['ReleaseForMarking'] />
							
							<#if student.stages?keys?seq_contains('FirstMarking')>
								<#if submission?? && submission.assignment??>
									<#if submission.firstMarker?has_content>
										<#local firstMarker><span data-profile="${submission.firstMarker.warwickId}">${submission.firstMarker.fullName}</span></#local>
									</#if>
								</#if>
							
								<@stage student.stages['FirstMarking']> <#compress>
									<#if firstMarker?default("")?length gt 0>(<#noescape>${firstMarker}</#noescape>)</#if>
								</#compress></@stage>
							</#if>
							
							<#if student.stages?keys?seq_contains('SecondMarking')>
								<#if submission?? && submission.assignment??>
									<#if submission.secondMarker?has_content>
										<#local secondMarker><span data-profile="${submission.secondMarker.warwickId}">${submission.secondMarker.fullName}</span></#local>
									</#if>
								</#if>
							
								<@stage student.stages['SecondMarking']> <#compress>
									<#if secondMarker?default("")?length gt 0>(<#noescape>${secondMarker}</#noescape>)</#if>
								</#compress></@stage>
							</#if>
						</div>
					</#if>
					
					<#if student.stages?keys?seq_contains('AddMarks') || student.stages?keys?seq_contains('AddFeedback')>
						<div class="stage-group">
							<h3>Feedback</h3>
							
							<div class="labels">
								<#if enhancedFeedback?? && feedback??>
									<#if feedback.released>
										<#if enhancedFeedback.downloaded><span class="label label-success">Downloaded</span>
										<#else><span class="label label-info">Published</span>
										</#if>
									<#else><span class="label label-warning">Not yet published</span>
									</#if>
								</#if>
							</div>
							
							<#-- If the current action is in this section, then add the next action blowout here -->
							<#if student.nextStage?? && ['AddMarks','AddFeedback','ReleaseFeedback','DownloadFeedback']?seq_contains(student.nextStage.toString)>
								<div class="alert pull-right">
									<strong>Next action:</strong> <@action student.nextStage.actionCode student />
								</div>
							</#if>
							
							<#if student.stages?keys?seq_contains('AddMarks')>
								<@stage student.stages['AddMarks']><#compress>
									<#if feedback?? && feedback.hasMarkOrGrade>
										: <#compress>
											<#if feedback.hasMark>
												${feedback.actualMark!''}<#if feedback.hasGrade>,</#if>
											</#if>
											<#if feedback.hasGrade>
												grade ${feedback.actualGrade!''}
											</#if>
										</#compress>
									</#if>
								</#compress></@stage>
							</#if>
							
							<#if student.stages?keys?seq_contains('AddFeedback')>
								<@stage student.stages['AddFeedback']><#compress>
									<#if feedback?? && feedback.hasAttachments>: <#compress>
										<#local attachments=feedback.attachments />
										<#if attachments?size gt 0>
											<#if attachments?size == 1> 
												<#local attachmentExtension = student.coursework.enhancedFeedback.feedback.attachments[0].fileExt>
											<#else>
												<#local attachmentExtension = "zip">
											</#if>
											<a class="long-running" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/feedback/download/${student.coursework.enhancedFeedback.feedback.id}/feedback-${student.coursework.enhancedFeedback.feedback.universityId}.${attachmentExtension}'/>"><#compress>
												${attachments?size}
												<#if attachments?size == 1> file
												<#else> files
												</#if>
											</#compress></a><#--
										--></#if><#--
										--><#if feedback.uploadedDate??> <#compress>
												<@fmt.date date=feedback.uploadedDate seconds=true capitalise=true shortMonth=true />
										</#compress></#if>
									</#compress></#if>
								</#compress></@stage>
							</#if>
							
							<#if student.stages?keys?seq_contains('ReleaseFeedback')>
								<@stage student.stages['ReleaseFeedback'] />
							</#if>
							
							<#if student.stages?keys?seq_contains('DownloadFeedback')>
								<@stage student.stages['DownloadFeedback'] />
							</#if>
						</div>
					</#if>
				</div>
			</#macro>
			
			<#macro row student>
				<tr class="itemContainer<#if !student.coursework.enhancedSubmission??> awaiting-submission</#if>"<#if student.coursework.enhancedSubmission?? && student.coursework.enhancedSubmission.submission.suspectPlagiarised> data-plagiarised="true"</#if>>
					<td class="check-col"><#if student.coursework.enhancedSubmission?? || student.coursework.enhancedFeedback??><input type="checkbox" class="collection-checkbox" name="students" value="${student.user.warwickId}"></#if></td>
					<#if department.showStudentName>
						<td class="student-col"><h6 data-profile="${student.user.warwickId}">${student.user.firstName}</h6></td>
						<td class="student-col"><h6 data-profile="${student.user.warwickId}">${student.user.lastName}</h6></td>
					<#else>
						<td class="student-col"><h6 data-profile="${student.user.warwickId}">${student.user.warwickId}</h6></td>
					</#if>
					<td class="progress-col">	
						<#if student.stages?keys?seq_contains('Submission') && student.nextStage?? && student.nextStage.toString != 'Submission' && student.stages['Submission'].messageCode != student.progress.messageCode>
							<#local progressTooltip><@spring.message code=student.stages['Submission'].messageCode />. <@spring.message code=student.progress.messageCode /></#local>
						<#else>
							<#local progressTooltip><@spring.message code=student.progress.messageCode /></#local>
						</#if>
					
						<dl class="progress progress-${student.progress.t} use-tooltip" title="${progressTooltip}" style="margin: 0; border-bottom: 0;" data-container="body">
							<dt class="bar" style="width: ${student.progress.percentage}%;"></dt>
							<dd style="display: none;" class="workflow-progress-container" data-profile="${student.user.warwickId}">
								<div id="workflow-${student.user.warwickId}" class="workflow-container">
									<@workflow student />
								</div>
							</dd>
						</dl>
					</td>
					<td class="action-col">
						<#if student.nextStage??>
							<#if student.progress.messageCode == 'workflow.Submission.unsubmitted.failedToSubmit'>
								Failed to submit
							<#else>
								<@action student.nextStage.actionCode student />
							</#if>
						<#elseif student.progress.percentage == 100>
							Complete
						</#if>
					</td>
				</tr>
			</#macro>
		
			<#list students as student>
				<@row student />
			</#list>
		</tbody>
	</table>
<#else>
	<#if submissionAndFeedbackCommand.filter.name == 'AllStudents'>
		<p>There are no submissions or feedbacks yet for this assignment.</p>
	<#else>
		<#macro filterDescription filter filterParameters><#compress>
			${filter.description}
			<#if filter.parameters?size gt 0>(<#compress>
				<#list filter.parameters as param>${param._1()}=${filterParameters[param._1()]}<#if param_has_next>, </#if></#list>
			</#compress>)</#if>
		</#compress></#macro>
	
		<p>There are no <@filterDescription submissionAndFeedbackCommand.filter submissionAndFeedbackCommand.filterParameters /> for this assignment.</p>
	</#if>
</#if>

<script type="text/javascript" src="/static/libs/jquery-tablesorter/jquery.tablesorter.min.js"></script>
<script type="text/javascript">
(function($) {
	
	
	var repositionWorkflowBoxes = function() {
		// These have to be positioned in the right order, so we loop through workflow-progress-container rather than directly on workflow
		$('.workflow-progress-container').each(function() {
			var universityId = $(this).attr('data-profile');
			var $workflow = $('#workflow-' + universityId);
			
			if ($workflow.length) {
				var isOpen = $workflow.data('open');
			
				if (isOpen) {
					var $progressField = $(this).closest('.progress-col');
					
					// Add bottom padding equivalent to the height of the workflow div to the progress field
					var fieldPosition = $progressField.position();
					
					$progressField.css('padding-bottom', '');
					var fieldHeight = $progressField.outerHeight();
					var workflowHeight = $workflow.outerHeight();
					$progressField.css('padding-bottom', workflowHeight + 10 + 'px');
					
					// Position the workflow div in the correct location
					$workflow.css({
						top: (fieldPosition.top + fieldHeight - 2) + 'px',
						left: ($('.students').position().left + 1) + 'px'
					});
				}
			}
		});
	};
		
	$('#navigation').on('fixed', repositionWorkflowBoxes);
	
	$.tablesorter.addWidget({
		id: 'repositionWorkflowBoxes',
		format: repositionWorkflowBoxes
	});
	
	// Expanding and contracting
	$('.students tbody tr').each(function() {
		var $nameField = $(this).find('.student-col h6').first();
		var $progressField = $(this).find('.progress-col').first();
		var $icon = $('<i class="icon-chevron-right"></i>').css('margin-top', '2px');
		
		$nameField
			.prepend(' ')
			.prepend($icon)
			.closest('tr')
			.find('.student-col,.progress-col')
			.css('cursor', 'pointer')
			.on('click', function(evt) {
				var universityId = $nameField.attr('data-profile');
				var $workflow = $('#workflow-' + universityId);
				if ($workflow.length) {
					var isOpen = $workflow.data('open');
				
					if (isOpen) {
						// Hide the workflow div and move it back to where it was before
						$workflow.hide();
						$progressField.find('dd').append($workflow);
						
						// Remove the bottom padding on the progress field
						$progressField.css('padding-bottom', '');
						
						// Remove any position data from the workflow div
						$workflow.attr('style', '');
						
						// Change the icon to closed
						$icon.removeClass('icon-chevron-down').addClass('icon-chevron-right');
						
						// Set the data
						$workflow.data('open', false); 
					} else {
						// Move the workflow div to be at the end of the offset parent and display it
						$('#main-content').append($workflow);
						$workflow.show();
						
						if ($progressField.closest('tr').is(':nth-child(odd)')) {
							$workflow.css('background-color', '#ebebeb');
						} else {
							$workflow.css('background-color', '#f5f5f5');
						}
						
						$workflow.css({
							width: ($('.students').width() - 21) + 'px'
						});
						
						// Change the icon to open
						$icon.removeClass('icon-chevron-right').addClass('icon-chevron-down');
						
						// Set the data
						$workflow.data('open', true);
					}
					
					repositionWorkflowBoxes();
					
					evt.preventDefault();
					evt.stopPropagation();
				}
			});
	});

	$('.students').tablesorter({
		sortList: [[<#if department.showStudentName>3<#else>2</#if>,0]],
		headers: { 0: { sorter: false } },
		textExtraction: function(node) {
			var $el = $(node);
			if ($el.hasClass('progress-col')) {
				var width = $el.find('.bar').width();
				
				// Add differing amounts for info, success, warning and danger so that they are sorted in the right order. 
				// info is the default, so == 0
				if ($el.find('.progress').hasClass('progress-success')) {
					width += 3;
				} else if ($el.find('.progress').hasClass('progress-warning')) {
					width += 2;
				} else if ($el.find('.progress').hasClass('progress-danger')) {
					width += 1;
				}

				return width;
			} else {
				return $el.text().trim();
			}
		},
		widgets: ['repositionWorkflowBoxes']
	});
})(jQuery);
</script>
<#else>
</div>
</#if>

</#escape>