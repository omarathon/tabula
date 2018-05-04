<#escape x as x?html>
	<#import "*/cm2_macros.ftl" as cm2 />
	<@cm2.assignmentHeader "Submit feedback" assignment "for" />
	<#assign commandName="addFeedbackCommand" />
	<@spring.bind path=commandName>
		<#assign hasErrors=status.errors.allErrors?size gt 0 />
		<#assign hasGlobalErrors=status.errors.globalErrors?size gt 0 />
	</@spring.bind>

	<#assign submitUrl><@routes.cm2.addFeedback assignment /></#assign>
	<@f.form method="post" action=submitUrl commandName=commandName cssClass="submission-form double-submit-protection">
		<input type="hidden" name="batch" value="true">
		<@spring.bind path="fromArchive"><#assign fromArchive=status.actualValue /></@spring.bind>
		<#if fromArchive>
			<#assign verbed_your_noun=".zip file has been unpacked"/>
		<#else>
			<#assign verbed_your_noun="files have been received"/>
		</#if>

		<@spring.bind path="items">
			<#assign itemsList=status.actualValue />
			<p>
				<#if itemsList?size gt 0>
					Your ${verbed_your_noun} with feedback for <@fmt.p itemsList?size "student"/>.
					<#if hasErrors>
						There are some problems, which are shown below.
						You need to correct these problems with the .zip file and try again.
					</#if>
				<#else>
						Your ${verbed_your_noun} but there are no files that look like feedback items.
				</#if>
			</p>
		</@spring.bind>

		<#if hasGlobalErrors>
			<div class="alert alert-danger"><@f.errors path="" cssClass="error"/></div>
		<#else>
			<#if addFeedbackCommand.unrecognisedFiles?size gt 0>
				<div class="alert alert-info">
					<div>The following files are not recognised and will be ignored:</div>
					<ul class="file-list">
						<#list addFeedbackCommand.unrecognisedFiles as unrecognisedFile>
							<li>
								<@f.hidden path="unrecognisedFiles[${unrecognisedFile_index}].path" />
								<@f.hidden path="unrecognisedFiles[${unrecognisedFile_index}].file" />
								${unrecognisedFile.path}
							</li>
						</#list>
					</ul>
				</div>
			</#if>
			<#if addFeedbackCommand.moduleMismatchFiles?size gt 0>
				<div class="invalid-files alert-block alert-info">
					<div>Some files, based on their names, appear to relate to another module. Please check these before confirming.</div>
					<ul class="file-list">
						<#list addFeedbackCommand.moduleMismatchFiles as moduleMismatchFile>
							<li>
								<@f.hidden path="moduleMismatchFiles[${moduleMismatchFile_index}].path" />
								<@f.hidden path="moduleMismatchFiles[${moduleMismatchFile_index}].file" />
								${moduleMismatchFile.path}
							</li>
						</#list>
					</ul>
				</div>
			</#if>

			<#if addFeedbackCommand.invalidFiles?size gt 0>
				<div class="alert alert-block alert-info">
				<div>Some files have problematic names. Fix them, then try uploading again.</div>
					<ul class="file-list">
						<#list addFeedbackCommand.invalidFiles as invalidFile>
							<li>
								<@f.hidden path="invalidFiles[${invalidFile_index}].path" />
								<@f.hidden path="invalidFiles[${invalidFile_index}].file" />
								${invalidFile.path}
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
										<#-- If there is nothing to upload hide these errors -->
										<#if item.modified>
											<#if item.submissionExists>
												<span class="warning">Feedback already exists for this student. New files will be added to the existing ones.</span>
											</#if>
											<#if item.published>
												<span class="warning">Feedback for this student has already been published. They will be notified that their feedback has changed.</span>
											</#if>
										</#if>
									</td>
									<#noescape>
										<@spring.bind path="file.attached" htmlEscape=false>
											<td>
												<ul class="file-list">
													<#list addFeedbackCommand.items[item_index].listAttachments as attached>
														<li>
															<@f.hidden path="file.attached[${attached_index}]" />
															${attached.name}
															<@f.errors path="file.attached[${attached_index}]" cssClass="error" />
															<#if attached.duplicate>
																<span class="alert-danger">
																	A feedback file with this name already exists for this student. It will be overwritten.
																</span>
															</#if>
															<#if attached.ignore>
																<span class="alert-info">
																	This feedback file has already been uploaded. It will be ignored.
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
			<#if hasErrors>
				<input class="btn btn-primary" type="submit" value="Confirm" disabled="disabled">
			<#else>
				<input type="hidden" name="confirm" value="true">
				<input class="btn btn-primary" type="submit" value="Confirm">
			</#if>
				<a href="<@routes.cm2.assignmentsubmissionsandfeedbacksummary assignment />" class="btn btn-default">Cancel</a>
		</div>
	</@f.form>

</#escape>