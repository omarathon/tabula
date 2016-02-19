<#escape x as x?html>
<#import "/WEB-INF/freemarker/modal_macros.ftl" as modal />
<#import "grid_macros.ftl" as grid />
<@modal.wrapper cssClass="modal-xl">
	<@modal.body>
		<div class="exam-grid-preview">
			<div class="overcat-key">
				<table class="table table-condensed">
					<thead>
						<tr>
							<th colspan="2">Key</th>
						</tr>
					</thead>
					<tbody>
						<tr>
							<td><span class="exam-grid-fail">#</span></td>
							<td>Failed module</td>
						</tr>
						<tr>
							<td><span class="exam-grid-override">#</span></td>
							<td>Manually adjusted and not stored in SITS</td>
						</tr>
						<tr>
							<td>#?</td>
							<td>Agreed mark missing (using actual mark)</td>
						</tr>
					</tbody>
				</table>
			</div>

			<h3>Select an overcatted mark for ${scyd.studentCourseDetails.student.fullName!} - ${scyd.studentCourseDetails.student.universityId}</h3>

			<p>You can change module marks but any changes made at this stage will not be saved back to SITS.</p>

			<form>
				<table class="table table-condensed grid overcat">
					<thead>
						<tr>
							<th colspan="${overcatView.optionsColumns?size}">Options</th>
							<td></td>
							<th colspan="${mapGet(overcatView.columnsBySCYD, scyd)?size}">${scyd.academicYear.toString} Modules (${scyd.studentCourseDetails.scjCode})</th>
							<#list overcatView.previousYearsSCYDs as thisSCYD>
								<td></td>
								<th colspan="${mapGet(overcatView.columnsBySCYD, thisSCYD)?size}">${thisSCYD.academicYear.toString} Modules (${thisSCYD.studentCourseDetails.scjCode})</th>
							</#list>
						</tr>
					</thead>
					<tbody>
						<tr class="category">
							<@grid.categoryRow categories=overcatView.optionsColumnsCategories columns=overcatView.optionsColumns showSectionLabels=false />
							<@grid.categoryRow categories=mapGet(overcatView.columnsBySCYDCategories, scyd) columns=mapGet(overcatView.columnsBySCYD, scyd) showSectionLabels=true />
							<#list overcatView.previousYearsSCYDs as thisSCYD>
								<td class="first-in-category"></td>
								<@grid.categoryRow categories=mapGet(overcatView.columnsBySCYDCategories, thisSCYD) columns=mapGet(overcatView.columnsBySCYD, thisSCYD) showSectionLabels=false />
							</#list>
						</tr>
						<tr class="title-in-category">
							<@grid.titleInCategoryRow categories=overcatView.optionsColumnsCategories columns=overcatView.optionsColumns showSectionLabels=false />
							<@grid.titleInCategoryRow categories=mapGet(overcatView.columnsBySCYDCategories, scyd) columns=mapGet(overcatView.columnsBySCYD, scyd) showSectionLabels=true />
							<#list overcatView.previousYearsSCYDs as thisSCYD>
								<td class="first-in-category"></td>
								<@grid.titleInCategoryRow categories=mapGet(overcatView.columnsBySCYDCategories, thisSCYD) columns=mapGet(overcatView.columnsBySCYD, thisSCYD) showSectionLabels=false />
							</#list>
						</tr>
						<tr>
							<@grid.headerRow columns=overcatView.optionsColumns showSectionLabels=false />
							<@grid.headerRow columns=mapGet(overcatView.columnsBySCYD, scyd) showSectionLabels=true />
							<#list overcatView.previousYearsSCYDs as thisSCYD>
								<td class="first-in-category"></td>
								<@grid.headerRow columns=mapGet(overcatView.columnsBySCYD, thisSCYD) showSectionLabels=false />
							</#list>
						</tr>
						<#list overcatView.overcattedEntities as entity>
							<tr class="student clickable">
								<#assign isFirstEntity = entity_index == 0 />
								<@grid.entityRows
									entity=entity
									isFirstEntity=isFirstEntity
									entityCount=overcatView.overcattedEntities?size
									columns=overcatView.optionsColumns
									columnValues=overcatView.optionsColumnValues
									showSectionLabels=false
								/>
								<@grid.entityRows
									entity=entity
									isFirstEntity=isFirstEntity
									entityCount=overcatView.overcattedEntities?size
									columns=mapGet(overcatView.columnsBySCYD, scyd)
									columnValues=mapGet(overcatView.columnsBySCYDValues, scyd)
									showSectionLabels=true
								/>
								<#list overcatView.previousYearsSCYDs as thisSCYD>
									<td class="first-in-category"></td>
									<@grid.entityRows
										entity=entity
										isFirstEntity=isFirstEntity
										entityCount=overcatView.overcattedEntities?size
										columns=mapGet(overcatView.columnsBySCYD, thisSCYD)
										columnValues=mapGet(overcatView.columnsBySCYDValues, thisSCYD)
										showSectionLabels=false
									/>
								</#list>
							</tr>
						</#list>
						<tr class="new-marks">
							<td colspan="${overcatView.optionsColumns?size}" class="borderless"></td>
							<th class="section-value-label">New Module Mark</th>
							<#assign currentCategory = "" />
							<#list mapGet(overcatView.columnsBySCYD, scyd) as column>
								<#if column.category?has_content && currentCategory != column.category>
									<#assign firstInCategory = true />
									<#assign currentCategory = column.category />
								<#else>
									<#assign firstInCategory = false />
								</#if>
								<td <#if firstInCategory!false>class="first-in-category"</#if>>
									<input type="text" class="form-control" name="newModuleMarks[${column.module.code}]" value="${mapGet(command.newModuleMarks, column.module)!}"/>
								</td>
							</#list>
						</tr>
						<tr>
							<td colspan="${overcatView.optionsColumns?size + 1}" class="borderless"></td>
							<#-- 1000 just so it spans the whole rest of the row -->
							<td colspan="1000" class="borderless">
								<button type="button" class="btn btn-default" name="recalculate">Recalculate</button>
								<button type="button" class="btn btn-default" name="reset">Reset</button>
							</td>
						</tr>
					</tbody>
				</table>
			</form>
		</div>
	</@modal.body>
	<@modal.footer>
		<div class="pull-left">
			<button type="button" class="btn btn-primary" name="continue">Continue</button>
			<button type="button" class="btn btn-default" name="cancel">Cancel</button>
		</div>
	</@modal.footer>
</@modal.wrapper>

<script>
	jQuery(function($){
		var $modalBody = $('.modal-body');
		$('.modal-body th.first-in-category, .modal-body td.first-in-category').each(function(){
			$(this).prev().addClass('last-in-category');
		});
		$('.modal-body th.rotated, .modal-body td.rotated').each(function() {
			var width = $(this).find('.rotate').width();
			var height = $(this).find('.rotate').height();
			$(this).css('height', width + 15).css('min-width', height + 5);
			$(this).find('.rotate').css({
				'margin-left': height / 2
			}).not('.nomargin').css({
				'margin-top': -(height)
			}).end().filter('.middle').not('.nomargin').css({
				'margin-top': width / 4
			});
		});
		$modalBody.wideTables();

		var updateButtons = function(){
			if ($('.modal-body input:checked').length > 0) {
				$('.modal-footer button[name=continue]').prop('disabled', false);
			} else {
				$('.modal-footer button[name=continue]').prop('disabled', true);
			}
			if ($('.modal-body tr.new-marks input').filter(function(){ return $(this).val().length > 0 }).length > 0) {
				$('.modal-body button[name=recalculate]').prop('disabled', false);
				$('.modal-body button[name=reset]').prop('disabled', false);
			} else {
				$('.modal-body button[name=recalculate]').prop('disabled', true);
				$('.modal-body button[name=reset]').prop('disabled', true);
			}
		};
		updateButtons();
		$('.modal-body table.grid').on('click', 'tr.clickable', function(){
			$(this).find('input').prop('checked', true);
			updateButtons();
		}).on('keyup', updateButtons);

		$modalBody.on('click', 'button[name=recalculate]', function(){
			$('.modal button').prop('disabled', true);
			var $form = $('.modal-body form');
			$form.append($('<input/>').attr({
				'type': 'hidden',
				'name': 'recalculate'
			}));
			$.post('<@routes.exams.generateGridOvercatting department academicYear scyd/>', $form.serialize(), function(data){
				$('#edit-overcatting-modal').html(data);
			});
		}).on('click', 'button[name=reset]', function(){
			$('.modal button').prop('disabled', true);
			var $form = $('.modal-body form');
			$form.append($('<input/>').attr({
				'type': 'hidden',
				'name': 'recalculate'
			})).find('tr.new-marks input').each(function(){ $(this).val(''); });
			$.post('<@routes.exams.generateGridOvercatting department academicYear scyd/>', $form.serialize(), function(data){
				$('#edit-overcatting-modal').html(data);
			});
		});
		$('.modal-footer').on('click', 'button[name=continue]', function(){
			$('.modal button').prop('disabled', true);
			var $form = $('.modal-body form');
			$form.append($('<input/>').attr({
				'type': 'hidden',
				'name': 'continue'
			}));
			$.post('<@routes.exams.generateGridOvercatting department academicYear scyd/>', $form.serialize(), function(data){
				if (!data.errors) {
					$('form.exam-grid-preview').append($('<input/>').attr({
						'type': 'hidden',
						'name': '${GenerateExamGridMappingParameters.previewAndDownload}'
					})).submit();
				} else {
					updateButtons();
				}
			});
		}).on('click', 'button[name=cancel]', function(){
			$(this).closest('.modal').modal('hide');
		});
	});
</script>

</#escape>