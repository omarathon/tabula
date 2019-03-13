<#escape x as x?html>
	<#import "*/cm2_macros.ftl" as cm2 />
	<@cm2.assignmentHeader "Submit feedback" assignment "for" />
	<#assign commandName="addMarkerFeedbackCommand" />
	<@spring.bind path=commandName>
		<#assign hasErrors=status.errors.allErrors?size gt 0 />
		<#assign hasGlobalErrors=status.errors.globalErrors?size gt 0 />
	</@spring.bind>

	<#assign submitUrl><@routes.cm2.uploadmarkerfeedback assignment marker /></#assign>
	<@f.form method="post" class="double-submit-protection"  action=submitUrl modelAttribute=commandName>
		<input type="hidden" name="batch" value="true">
			<@spring.bind path="fromArchive"><#assign fromArchive=status.actualValue /></@spring.bind>
			<#if fromArchive>
				<#assign noun_verb_passive=".zip file has been unpacked"/>
			<#else>
				<#assign noun_verb_passive="files have been received"/>
			</#if>

			<@spring.bind path="items">
				<#assign itemsList=status.actualValue />
				<p>
					<#if itemsList?size gt 0>
						Your ${noun_verb_passive} with feedback for <@fmt.p number=itemsList?size singular="student" plural="students" shownumber=true />
						<#if hasErrors>
							<div class="alert alert-danger">
								There are some problems, which are shown below.
								You need to correct these problems with the .zip file and try again.
							</div>
						</#if>
					<#else>
						<div class="alert alert-danger">
							Your ${noun_verb_passive} but there are no files that look like feedback items.
						</div>
					</#if>
				</p>
			</@spring.bind>

			<#if hasGlobalErrors>
				<div class="alert alert-danger"><@f.errors path="" cssClass="error"/></div>
			<#else>
				<#if addMarkerFeedbackCommand.unrecognisedFiles?size gt 0>
					<div class="unrecognised-files alert alert-block">
						<div>The following files are not recognised and will be ignored:</div>
							<ul class="file-list">
								<#list addMarkerFeedbackCommand.unrecognisedFiles as unrecognisedFile>
									<li>
											<@f.hidden path="unrecognisedFiles[${unrecognisedFile_index}].path" />
											<@f.hidden path="unrecognisedFiles[${unrecognisedFile_index}].file" />
											${unrecognisedFile.path}
									</li>
								</#list>
							</ul>
					</div>
				</#if>

				<#if addMarkerFeedbackCommand.moduleMismatchFiles?size gt 0>
					<div class="invalid-files alert alert-danger">
						<div>Some files, based on their names, appear to relate to another module. Please check these before confirming.</div>
							<ul class="file-list">
								<#list addMarkerFeedbackCommand.moduleMismatchFiles as moduleMismatchFile>
									<li>
										<@f.hidden path="moduleMismatchFiles[${moduleMismatchFile_index}].path" />
											<@f.hidden path="moduleMismatchFiles[${moduleMismatchFile_index}].file" />
											${moduleMismatchFile.path}
									</li>
								</#list>
							</ul>
					</div>
				</#if>
				<#if addMarkerFeedbackCommand.invalidFiles?size gt 0>
					<div class="invalid-files alert alert-block alert-danger">
						<div>Some files have problematic names. Fix them, then try uploading again.</div>
							<ul class="file-list">
								<#list addMarkerFeedbackCommand.invalidFiles as invalidFile>
									<li>
										<@f.hidden path="invalidFiles[${invalidFile_index}].path" />
											<@f.hidden path="invalidFiles[${invalidFile_index}].file" />
											${invalidFile.path}
									</li>
								</#list>
							</ul>
					</div>
				</#if>
				<#if addMarkerFeedbackCommand.invalidStudents?size gt 0>
					<div class="invalid-students alert">
						<div>Some of the feedback you uploaded is for students that are not allocated to you for marking. These files will be ignored.</div>
							<ul class="file-list">
								<#list addMarkerFeedbackCommand.invalidStudents as invalidStudent>
									<li>
										${invalidStudent.uniNumber}
									</li>
								</#list>
							</ul>
					</div>
				</#if>
				<#if addMarkerFeedbackCommand.markedStudents?size gt 0>
					<div class="invalid-students alert">
						<div>Some of the feedback that you uploaded is for students that you have finished marking. These files will be ignored.</div>
						<ul class="file-list">
							<#list addMarkerFeedbackCommand.markedStudents as markedStudent>
								<li>
									${markedStudent.uniNumber}
								</li>
							</#list>
						</ul>
					</div>
				</#if>
				<@spring.bind path="items">
					<#assign itemList=status.actualValue />
					<#if itemList?size gt 0>
						<table class="table table-striped">
							<tr>
								<th>University ID</th>
								<th>Files</th>
							</tr>
							<#list itemList as item>
								<tr>
									<@spring.nestedPath path="items[${item_index}]">
										<@f.hidden path="uniNumber" />
										<td>
											<@spring.bind path="uniNumber">
											${status.value}
											</@spring.bind>
											<@f.errors path="uniNumber" cssClass="error" />
											<#if item.submissionExists>
												<span class="warning">Feedback already exists for this student. New files will be added to the existing ones.</span>
											</#if>
										</td>
										<#noescape>
											<@spring.bind path="file.attached" htmlEscape=false>
												<td>
													<ul class="file-list">
														<#list addMarkerFeedbackCommand.items[item_index].listAttachments as attached>
															<li>
																<@f.hidden path="file.attached[${attached_index}]" />
																	${attached.name}
																<@f.errors path="file.attached[${attached_index}]" cssClass="error" />
																<#if attached.duplicate>
																	<span class="warning">
																		A feedback file with this name already exists for this student. It will be overwritten.
																	</span>
																</#if>
															</li>
														</#list>
													</ul>
												</td>
											</@spring.bind>
										</#noescape>
									</@spring.nestedPath>
								</tr>
							</#list>
						</table>
					</#if>
				</@spring.bind>
			</#if>
			<div class="submit-buttons form-actions">
				<#if hasErrors || itemList?size == 0>
					<input class="btn btn-primary" type="submit" value="Confirm" disabled="disabled">
				<#else>
					<input type="hidden" name="confirm" value="true">
					<input class="btn btn-primary" type="submit" value="Confirm">
				</#if>
				<a href="<@routes.cm2.listmarkersubmissions assignment marker />" class="btn btn-default">Cancel</a>
			</div>
	</@f.form>
</#escape>
