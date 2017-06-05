<#import "*/cm2_macros.ftl" as cm2 />
<#escape x as x?html>
<@cm2.assignmentHeader "Submit marks and feedback" assignment />
<div>
	<!-- Nav tabs -->
	<ul class="nav nav-tabs" role="tablist">
		<li role="presentation" class="active"><a href="#upload" aria-controls="upload" role="tab" data-toggle="tab">Upload</a></li>
		<li role="presentation"><a href="#webform" aria-controls="webform" role="tab" data-toggle="tab">Web Form</a></li>
	</ul>
	<!-- Tab panes -->
	<div class="tab-content">
		<div role="tabpanel" class="tab-pane active" id="upload">
			<p>
				You can upload marks and feedback in a spreadsheet, which must be saved as an XLSX file (i.e. created in Microsoft Office 2007 or later).
				The spreadsheet should have the following column headings: ID, Mark, Grade and Feedback.
				You can use this <a href="${templateUrl}" >generated spreadsheet</a> as a template.
				Note that you can upload just marks, marks and grades, or marks, grades and feedback.
			</p>
		<@f.form method="post" enctype="multipart/form-data" action="${formUrl}" commandName="command">
			<h3>Select file</h3>
			<p id="multifile-column-description">
				<#include "/WEB-INF/freemarker/multiple_upload_help.ftl" />
			</p>
			<@bs3form.labelled_form_group path="file.upload" labelText="Files">
				<input type="file" name="file.upload" multiple/>
			</@bs3form.labelled_form_group>
			<@f.errors path="file" cssClass="error" />
			<input type="hidden" name="isfile" value="true" />

			<div class="buttons form-group">
				<button type="submit" class="btn btn-primary">Upload</button>
				<a class="btn btn-default cancel" href="${cancelUrl}">Cancel</a>
			</div>
		</@f.form>
		</div>
		<div role="tabpanel" class="tab-pane" id="webform">
			<p>Click the add button below to enter marks and feedback for a student.</p>
		<@f.form cssClass="marks-web-form" method="post" enctype="multipart/form-data" action="${formUrl}" commandName="command">
			<input type="hidden" name="isfile" value="true" />
			<div class="form-group">
				<button class="add-mark-row btn btn-default">+ Add</button>
			</div>
			<div class="buttons form-group">
				<button type="submit" class="btn btn-primary">Save</button>
				<a class="btn btn-default cancel" href="${cancelUrl}">Cancel</a>
			</div>
		</@f.form>
			<div class="hidden mark-row">
				<div class="row">
					<div class="col-md-2">
						<div class="form-group">
							<input name="id" class="form-control" type="text" placeholder="ID">
						</div>
					</div>
					<div class="col-md-2">
						<div class="form-group">
							<div class="input-group">
								<input name="actualMark" class="form-control" type="number" placeholder="Mark"><div class="input-group-addon">%</div>
							</div>
						</div>
					</div>
					<div class="col-md-2">
						<div class="form-group">
							<input name="actualGrade" class="form-control" type="text" placeholder="Grade">
						</div>
					</div>
					<div class="col-md-6">
						<div class="form-group">
							<textarea class="small-textarea form-control" name="feedbackComment" placeholder="Feedback"></textarea>
						</div>
					</div>
				</div>
			</div>
		</div>
		</div>
	</div>

	<script type="text/javascript">
		(function($) {
			var $form = $('form.marks-web-form');
			var $rowTemplate = $('div.mark-row .row');

			$('.add-mark-row').on('click', function(e){
				e.preventDefault();
				var numExistingRows = $('.marks-web-form div.row').size();
				var $newRow = $rowTemplate.clone();
				$newRow.find('input,textarea').each(function(i, field){
					var newName = 'marks['+ numExistingRows +'].' + $(field).attr('name');
					$(field).attr('name', newName);
				});
				$form.prepend($newRow);
			});
		})(jQuery);
	</script>
</#escape>