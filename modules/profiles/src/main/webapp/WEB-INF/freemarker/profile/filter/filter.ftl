<#escape x as x?html>
	
	<h1>Students in ${department.name}</h1>
	
	<div class="persist-area">
		<div class="persist-header">
			<#assign submit_url><@routes.filter_students department /></#assign>
			<@f.form commandName="filterStudentsCommand" action="${submit_url}" method="GET" cssClass="form-inline">
				<@f.errors cssClass="error form-errors" />
				<@f.hidden path="page" />
				<@f.hidden path="studentsPerPage" />
			
				<div class="btn-group-group">
					<i class="icon-filter"></i> Filter:
					
					<#macro filter path name currentFilter allItems>
						<@spring.bind path=path>
							<div class="btn-group<#if !status.actualValue?has_content> empty-filter</#if>">	
								<a class="btn btn-mini dropdown-toggle" data-toggle="dropdown">
									<span class="subtler">${name}:</span> <span class="filter-short-values">${currentFilter}</span>
									<span class="caret"></span>
								</a>
								<div class="dropdown-menu filter-list">
									<ul>
										<#list allItems as item>
											<li class="check-list-item">
												<label class="checkbox">
													<#nested item />
												</label>
											</li>
										</#list>
									</ul>
								</div>
							</div>
						</@spring.bind>
					</#macro>
					
					<#macro current_filter_value path><#compress>
						<@spring.bind path=path>
							<#if status.actualValue?has_content>
								<#list status.actualValue as item><#nested item /><#if item_has_next>, </#if></#list>
							<#else>
								Any
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
					
					<#assign current_course_types_filter><@current_filter_value "courseTypes"; courseType>${courseType.code}</@current_filter_value></#assign>
					<@filter "courseTypes" "Course type" current_course_types_filter filterStudentsCommand.allCourseTypes; courseType>
						<input type="checkbox" name="${status.expression}" value="${courseType.code}" data-short-value="${courseType.code}" ${contains_by_code(filterStudentsCommand.courseTypes, courseType)?string('checked="checked"','')}>
						${courseType.description}
					</@filter>
					
					<#assign current_routes_filter><@current_filter_value "routes"; route>${route.code?upper_case}</@current_filter_value></#assign>
					<@filter "routes" "Route" current_routes_filter filterStudentsCommand.allRoutes; route>
						<input type="checkbox" name="${status.expression}" value="${route.code}" data-short-value="${route.code?upper_case}" ${contains_by_code(filterStudentsCommand.routes, route)?string('checked="checked"','')}>
						<@fmt.route_name route false />
					</@filter>
					
					<#assign current_moa_filter><@current_filter_value "modesOfAttendance"; moa>${moa.shortName?capitalize}</@current_filter_value></#assign>
					<@filter "modesOfAttendance" "Attendance" current_moa_filter filterStudentsCommand.allModesOfAttendance; moa>
						<input type="checkbox" name="${status.expression}" value="${moa.code}" data-short-value="${moa.shortName?capitalize}" ${contains_by_code(filterStudentsCommand.modesOfAttendance, moa)?string('checked="checked"','')}>
						${moa.fullName}
					</@filter>
					
					<#assign current_years_filter><@current_filter_value "yearsOfStudy"; year>${year}</@current_filter_value></#assign>
					<@filter "yearsOfStudy" "Year of study" current_years_filter filterStudentsCommand.allYearsOfStudy; yearOfStudy>
						<input type="checkbox" name="${status.expression}" value="${yearOfStudy}" data-short-value="${yearOfStudy}" ${filterStudentsCommand.yearsOfStudy?seq_contains(yearOfStudy)?string('checked="checked"','')}>
						${yearOfStudy}
					</@filter>
					
					<#assign current_statuses_filter><@current_filter_value "sprStatuses"; sprStatus>${sprStatus.shortName?capitalize}</@current_filter_value></#assign>
					<@filter "sprStatuses" "Status" current_statuses_filter filterStudentsCommand.allSprStatuses; sprStatus>
						<input type="checkbox" name="${status.expression}" value="${sprStatus.code}" data-short-value="${sprStatus.shortName?capitalize}" ${contains_by_code(filterStudentsCommand.sprStatuses, sprStatus)?string('checked="checked"','')}>
						${sprStatus.fullName}
					</@filter>
					
					<#assign current_modules_filter><@current_filter_value "modules"; module>${module.code?upper_case}</@current_filter_value></#assign>
					<@filter "modules" "Module" current_modules_filter filterStudentsCommand.allModules; module>
						<input type="checkbox" name="${status.expression}" value="${module.code}" data-short-value="${module.code?upper_case}" ${contains_by_code(filterStudentsCommand.modules, module)?string('checked="checked"','')}>
						<@fmt.module_name module false />
					</@filter>
				</div>
			</@f.form>
		</div>
	
		<div id="filter-results">
			<#include "results.ftl" />
		</div>
	</div>
	
	<script type="text/javascript">
		jQuery(function($) {
			$('.persist-area').fixHeaderFooter();
		
			$('#filterStudentsCommand input').on('change', function(e) {
				// Load the new results
				var $form = $(this).closest('form');
				if ($form.data('request')) {
					$form.data('request').abort();
					$form.data('request', null);
				}
				
				$('#filter-results').addClass('loading');
				$form.data('request', $.post($form.attr('action'), $form.serialize(), function(data) {
					$('#filter-results').html(data);
				
					$form.data('request', null);
					$('#filter-results').removeClass('loading');
				}));
				
				// Update the filter content
				var $list = $(this).closest('ul');
				var shortValues = $list.find(':checked').map(function() { return $(this).data('short-value'); }).get();
				if (shortValues.length) {
					$(this).closest('.btn-group').removeClass('empty-filter');
					$(this).closest('.btn-group').find('.filter-short-values').html(shortValues.join(', '));
				} else {
					$(this).closest('.btn-group').addClass('empty-filter');
					$(this).closest('.btn-group').find('.filter-short-values').html('Any');
				}
			});
		});
	</script>
	
</#escape>