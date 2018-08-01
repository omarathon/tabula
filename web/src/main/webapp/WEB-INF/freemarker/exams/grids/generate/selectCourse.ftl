<#import 'form_fields.ftl' as form_fields />
<#escape x as x?html>

<#function route_function dept>
	<#local selectCourseCommand><@routes.exams.generateGrid dept academicYear /></#local>
	<#return selectCourseCommand />
</#function>

<@fmt.id7_deptheader title="Create a new exam grid for ${department.name}" route_function=route_function />

<h2>Select courses for this grid</h2>

<p class="progress-arrows">
	<span class="arrow-right active">Select courses</span>
	<span class="arrow-right arrow-left">Set grid options</span>
	<span class="arrow-right arrow-left">Preview and download</span>
</p>

<div class="alert alert-info">
	<h3>Before you start</h3>
	<p>Exam grids in Tabula are generated using data stored in SITS.
		Before you create a new grid, ensure you have entered all the necessary data in SITS and verified its accuracy.</p>
</div>

<form action="<@routes.exams.generateGrid department academicYear />" class="form-inline select-course" method="post">

	<@form_fields.grid_options_fields />

	<div class="well well-sm filters">
		<button type="button" class="clear-all-filters btn btn-link">
			<span class="fa-stack">
				<i class="fa fa-filter fa-stack-1x"></i>
				<i class="fa fa-ban fa-stack-2x"></i>
			</span>
		</button>

		<#macro filter path placeholder currentFilter allItems prefix="" customPicker="" cssClass="" emptyText="N/A for this department">
			<@spring.bind path=path>
				<div class="btn-group<#if currentFilter == placeholder> empty-filter</#if> ${cssClass}">
					<a class="btn btn-default btn-mini btn-xs dropdown-toggle" data-toggle="dropdown">
						<span class="filter-short-values" data-placeholder="${placeholder}" data-prefix="${prefix}"><#if currentFilter != placeholder>${prefix}</#if>${currentFilter}</span>
						<span class="caret"></span>
					</a>
					<div class="dropdown-menu filter-list">
						<button type="button" class="close" data-dismiss="dropdown" aria-hidden="true" title="Close">×</button>
						<ul>
							<#if customPicker?has_content>
								<li>
									${customPicker}
								</li>
							</#if>
							<#if allItems?has_content>
								<#list allItems as item>
									<li class="check-list-item" data-natural-sort="${item_index}">
										<#nested item />
										<label class="checkbox">

										</label>
									</li>
								</#list>
							<#else>
								<li><div class="small muted" style="padding-left: 5px;">${emptyText}</div></li>
							</#if>
						</ul>
					</div>
				</div>
			</@spring.bind>
		</#macro>

		<#macro current_filter_value path placeholder><#compress>
			<@spring.bind path=path>
				<#if status.actualValue?has_content>
					<#if status.actualValue?is_collection>
						<#list status.actualValue as item><#nested item /><#if item_has_next>, </#if></#list>
					<#else>
						<#nested status.actualValue />
					</#if>
				<#else>
					${placeholder}
				</#if>
			</@spring.bind>
		</#compress></#macro>

		<#function contains_by_code collection item>
			<#list collection as c>
				<#if c.code == item.code>
					<#return true />
				</#if>
			</#list>
			<#return false />
		</#function>

		<#assign placeholder = "Course" />
		<#assign currentfilter><#compress><@current_filter_value "selectCourseCommand.courses" placeholder; course>
			<#if course?is_sequence>
				<#list course as c>${c.code?upper_case}<#if c_has_next>, </#if></#list>
			<#else>
				${course.code?upper_case}
			</#if>
		</@current_filter_value></#compress></#assign>

		<@filter "selectCourseCommand.courses" placeholder currentfilter selectCourseCommand.allCourses; course>
			<label class="checkbox">
				<input type="checkbox" name="${status.expression}" value="${course.code}" data-short-value="${course.code}"
					${contains_by_code(selectCourseCommand.courses, course)?string('checked','')}
				>
				${course.code} ${course.name}
			</label>
		</@filter>

		<#assign placeholder = "All routes" />

		<#assign currentfilter><#compress><@current_filter_value "selectCourseCommand.routes" placeholder; route>
			<#if route?is_sequence>
				<#list route as r>${r.code?upper_case}<#if r_has_next>, </#if></#list>
			<#else>
				${route.code?upper_case}
			</#if>
		</@current_filter_value></#compress></#assign>
		<@filter path="selectCourseCommand.routes" placeholder=placeholder currentFilter=currentfilter allItems=selectCourseCommand.allRoutes emptyText="No course routes found"; route>
			<label class="checkbox">
				<input type="checkbox" name="${status.expression}" value="${route.code}" data-short-value="${route.code?upper_case}"
					${contains_by_code(selectCourseCommand.routes, route)?string('checked','')}
				>
				${route.code?upper_case} ${route.name}
			</label>
		</@filter>

		<#-- level grids only for CAL at this point -->
		<#if department.code == 'et'>
			<div class="btn-group" style="margin: 0 10px;">
				Generate grid on:&nbsp;
				<label class="radio-inline">
					<input type="radio" name="gridScope" value="block"> Block
				</label>
				<label class="radio-inline">
					<input type="radio" name="gridScope" value="level"> Level
				</label>
			</div>
		</#if>

		<#assign placeholder = "Year of study" />
		<#assign currentfilter><@current_filter_value "selectCourseCommand.yearOfStudy" placeholder; year>${year}</@current_filter_value></#assign>
		<@filter path="selectCourseCommand.yearOfStudy" placeholder=placeholder currentFilter=currentfilter allItems=selectCourseCommand.allYearsOfStudy prefix="Year " cssClass="block" ; yearOfStudy>
			<label class="radio">
				<input type="radio" name="${status.expression}" value="${yearOfStudy}" data-short-value="${yearOfStudy}"
					${(((selectCourseCommand.yearOfStudy)!0) == yearOfStudy)?string('checked','')}
				>
				${yearOfStudy}
			</label>
		</@filter>

		<#if department.code == 'et'>
			<#assign placeholder = "Study level" />
			<#assign currentfilter><@current_filter_value "selectCourseCommand.levelCode" placeholder; levelCode>${levelCode}</@current_filter_value></#assign>
			<@filter path="selectCourseCommand.levelCode" placeholder=placeholder currentFilter=currentfilter allItems=selectCourseCommand.allLevels prefix="Level " cssClass="level"; level>
				<label class="radio">
					<input type="radio" name="${status.expression}" value="${level.code}" data-short-value="${level.code}"
						${(((selectCourseCommand.levelCode)!'') == level.code)?string('checked','')}
					>
					${level.code} - ${level.name}
				</label>
			</@filter>
		</#if>

	</div>
	<#assign studyYear = (selectCourseCommand.studyYearByLevelOrBlock)!0 />
	<div class="row year_info">
		<h3 class="year_info_hdr <#if studyYear == 0>hidden</#if>">Years to display on grid</h3>
		<#assign maxYear=selectCourseCommand.allYearsOfStudy?size>
			<#list 1..maxYear as counter>
				<div class="col-sm-2 year${counter} <#if counter gt studyYear>hidden</#if>" data-year="${counter}">
					<div class="checkbox">
						<#assign yearColumn="Year${counter}"/>
						<label>
							<input type="checkbox" name="courseYearsToShow" value="${yearColumn}"
								<#if selectCourseCommand.courseYearsToShow?seq_contains("${yearColumn}")>checked</#if> <#if studyYear == counter>disabled</#if>
							/> Year ${counter}
						</label>
					</div>
				</div>
				<#if studyYear == counter><input type="hidden" name="courseYearsToShow" value="${yearColumn}"/></#if>
			</#list>
			<#if studyYear == 0><input type="hidden" name="courseYearsToShow" value=""/></#if>
	</div>
	<div class="year_info_ftr <#if studyYear == 0>hidden</#if>"><hr/></div>

	<p>
		<div class="row">
			<div class="col-md-4">
				<@bs3form.checkbox path="selectCourseCommand.includeTempWithdrawn">
					<@f.checkbox path="selectCourseCommand.includeTempWithdrawn" /> Show temporarily withdrawn students
				</@bs3form.checkbox>
			</div>
			<div class="col-md-4">
				<@bs3form.checkbox path="selectCourseCommand.resitOnly">
					<@f.checkbox path="selectCourseCommand.resitOnly" /> Show only resit students
				</@bs3form.checkbox>
			</div>
		</div>
	</p>
	<p>
		<@bs3form.checkbox path="selectCourseCommand.includePermWithdrawn">
			<@f.checkbox path="selectCourseCommand.includePermWithdrawn" /> Show permanently withdrawn students
		</@bs3form.checkbox>
	</p>

	<@bs3form.errors path="selectCourseCommand" />

	<div class="buttons">

		<button class="btn btn-default" name="${GenerateExamGridMappingParameters.selectCourse}" type="submit" disabled>Configure grid options</button>

		<button class="btn btn-default" name="${GenerateExamGridMappingParameters.usePreviousSettings}" type="submit" disabled>Generate using previous settings</button>

		<#assign popover>
			<ul>
				<#list gridOptionsCommand.predefinedColumnDescriptions as column>
					<li>${column}</li>
				</#list>
				<#list gridOptionsCommand.customColumnTitles as column>
					<li>Additional: ${column}</li>
				</#list>
				<#if gridOptionsCommand.nameToShow.toString == 'full'>
					<li>Official name</li>
				<#elseif gridOptionsCommand.nameToShow.toString == 'both'>
					<li>First and last name</li>
				<#else>
					<li>No name</li>
				</#if>
				<#if gridOptionsCommand.marksToShow == 'overall'>
					<li>Only show overall mark</li>
				<#else>
					<li>Show component marks</li>
					<#if gridOptionsCommand.componentsToShow == 'all'>
						<li>Show all assessment components</li>
					<#else>
						<li>Hide zero weighted assessment components</li>
					</#if>
					<#if gridOptionsCommand.componentsToShow == 'markOnly'>
						<li>Only show component marks</li>
					<#else>
						<li>Show component marks and the sequence that they relate to</li>
					</#if>
				</#if>
				<#if gridOptionsCommand.moduleNameToShow.toString == 'nameAndCode'>
					<li>Show module names</li>
				<#elseif gridOptionsCommand.moduleNameToShow.toString == 'shortNameAndCode'>
					<li>Show module short names</li>
				<#else>
					<li>Show module code only</li>
				</#if>
				<#if gridOptionsCommand.layout == 'full'>
					<li>Full grid</li>
				<#else>
					<li>Short grid</li>
				</#if>
				<#if gridOptionsCommand.yearMarksToUse == 'sits'>
					<li>Uploaded year marks</li>
				<#else>
					<li>Calculate year marks</li>
				</#if>
			</ul>
		</#assign>

		<@fmt.help_popover id="gridOptions" title="Previous grid options" content=popover html=true />
	</div>
</form>

<script>
	jQuery(function($){
		var prependClearLink = function($list) {
			if (!$list.find('input:checked').length) {
				$list.find('.clear-this-filter').remove();
			} else {
				if (!$list.find('.clear-this-filter').length) {
					$list.find('> ul').prepend(
						$('<li />').addClass('clear-this-filter')
							.append(
								$('<button />').attr('type', 'button')
									.addClass('btn btn-link')
									.html('<i class="fa fa-ban"></i> Clear selected items')
									.on('click', function(e) {
										$list.find('input:checked').each(function() {
											var $checkbox = $(this);
											$checkbox.prop('checked', false);
											updateFilter($checkbox);
										});
										hideYearCheckboxesArea();
									})
							)
							.append($('<hr />'))
					);
				}
			}
		};

		var updateFilter = function($el) {
			// Update the filter content
			var $list = $el.closest('ul');
			var shortValues = $list.find(':checked').map(function() { return $(this).data('short-value'); }).get();
			var $fsv = $el.closest('.btn-group').find('.filter-short-values');
			if (shortValues.length) {
				$el.closest('.btn-group').removeClass('empty-filter');
				$fsv.html($fsv.data("prefix") + shortValues.join(', '));
			} else {
				$el.closest('.btn-group').addClass('empty-filter');
				$fsv.html($fsv.data('placeholder'));
			}

			updateButtons($el);
		};

		var updateButtons = function($el) {
			var $filterList = $el.closest(".filters");

			if ($filterList.find(".empty-filter").length == $filterList.find(".btn-group").length) {
				$('.clear-all-filters').attr("disabled", "disabled");

			} else {
				$('.clear-all-filters').removeAttr("disabled");
			}

			var course = $('[name=courses]:checked');
			var yearOfStudy = $('[name=yearOfStudy]:checked');
			var studyLevel = $('[name=levelCode]:checked');

			if (course.length === 0 || (yearOfStudy.length === 0 && studyLevel.length === 0)) {
				$('.buttons button.btn-default').prop('disabled', true);
			} else {
				$('.buttons button.btn-default').prop('disabled', false);
			}
		};

		var yearCheckboxes = function() {
			var yearOfStudy = $("input[type='radio'][name='yearOfStudy']:checked, input[type='radio'][name='levelCode']:checked");
			var selectedYearOfStudy = 0;
			$('.year_info .year_info_hdr').toggleClass("hidden", yearOfStudy.length === 0);
			$('.year_info_ftr').toggleClass("hidden", yearOfStudy.length === 0);
			if (yearOfStudy.length > 0) {
				selectedYearOfStudy = isNaN(Number(yearOfStudy.val())) ? 1 : yearOfStudy.val();
				$('.year_info').find("input[type='hidden'][name='courseYearsToShow']").val("Year"+selectedYearOfStudy);
			} else {
				return;
			}
			$('.year_info .col-sm-2').each(function(){
				var $yearCheckboxDiv =  $(this);
				var $yearCheckbox = $yearCheckboxDiv.find('input');
				var year = $yearCheckboxDiv.data('year');
				if(year == selectedYearOfStudy) {
					$yearCheckbox.prop("checked", true);
				} else if (year > selectedYearOfStudy) {
					$yearCheckbox.prop("checked", false);
				}
				$yearCheckbox.prop("disabled", (selectedYearOfStudy ==  year));
				$yearCheckboxDiv.toggleClass("hidden", selectedYearOfStudy <  year);
			});
		};

		var hideYearCheckboxesArea = function() {
			$('.year_info .year_info_hdr').addClass("hidden");
			$('.year_info_ftr').addClass("hidden");
			$('.year_info .col-sm-2').each(function(){
				var $yearCheckboxDiv =  $(this);
				var $yearCheckbox = $yearCheckboxDiv.find('input');
				$yearCheckboxDiv.addClass("hidden");
				$yearCheckbox.prop("checked", false);
			});
		};

		$('form.select-course .filters').on('change', function(e) {
			updateFilter($(e.target));
			yearCheckboxes();
		});
		$('form.select-course .filters .filter-list').find('input:first').trigger('change');

		// Re-order elements inside the dropdown when opened
		$('.filter-list').closest('.btn-group').find('.dropdown-toggle').on('click.dropdown.data-api', function(e) {
			var $this = $(this);
			if (!$this.closest('.btn-group').hasClass('open')) {
				// Re-order before it's opened!
				var $list = $this.closest('.btn-group').find('.filter-list');
				var items = $list.find('li.check-list-item').get();

				items.sort(function(a, b) {
					var aChecked = $(a).find('input').is(':checked');
					var bChecked = $(b).find('input').is(':checked');

					if (aChecked && !bChecked) return -1;
					else if (!aChecked && bChecked) return 1;
					else return $(a).data('natural-sort') - $(b).data('natural-sort');
				});

				$.each(items, function(item, el) {
					$list.find('> ul').append(el);
				});

				prependClearLink($list);
			}
		});

		$('.clear-all-filters').on('click', function() {
			$('.filter-list').each(function() {
				var $list = $(this);

				$list.find('input:checked').each(function() {
					var $checkbox = $(this);
					$checkbox.prop('checked', false);
					updateFilter($checkbox);
				});

				prependClearLink($list);
			});
			hideYearCheckboxesArea();
		});

		<#if department.code == 'et'>
			$('.level,.block').hide();
			var $gridScopeRadio = $('input[name=gridScope]');
			$gridScopeRadio.on('change', function(){
				if(this.value === "level"){ $('.block').hide(); $('.level').show(); }
				else if (this.value === "block") { $('.level').hide(); $('.block').show(); }
			});
		</#if>

	});
</script>
</#escape>