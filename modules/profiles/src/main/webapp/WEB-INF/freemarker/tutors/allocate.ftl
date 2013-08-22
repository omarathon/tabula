<#assign department=allocateStudentsToTutorsCommand.department />
<#assign mappingById=allocateStudentsToTutorsCommand.mappingById />
<#assign membersById=allocateStudentsToTutorsCommand.membersById />



<#macro student_item profile bindpath="">
	<li class="student well well-small"
	data-f-gender="${(profile.gender.dbValue)!}"
	data-f-route="${(profile.mostSignificantCourseDetails.route.code)!}"
	data-f-year="${(profile.mostSignificantCourseDetails.latestStudentCourseYearDetails.yearOfStudy)!}">
		<div class="profile clearfix">
			<@fmt.member_photo profile "tinythumbnail" false />
			<div class="name">
				<h6>${profile.fullName}</h6>
				${profile.mostSignificantCourseDetails.route.name}
			</div>
		</div>
		<input type="hidden" name="${bindpath}" value="${profile.universityId}" />
	</li>
</#macro>

<#escape x as x?html>
	<h1>Allocate students to personal tutors</h1>
	
	<noscript>
		<div class="alert">This page requires Javascript.</div>
	</noscript>

	<div class="tabbable">
		<ul class="nav nav-tabs">
			<li class="active">
				<a href="#allocatetutors-tab1" data-toggle="tab">Drag and drop</a>
			</li>
			<li >
				<a href="#allocatetutors-tab2" data-toggle="tab">Upload spreadsheet</a>
			</li>
		</ul>

		<div class="tab-content">
			<div id="allocatetutors-tab1" class="tab-pane active">

			<p>Drag students onto a tutor to allocate them to the tutor. Select multiple students by dragging a box around them.
				 You can also hold the <kbd class="keyboard-control-key">Ctrl</kbd> key to add to a selection.</p>

			<@spring.hasBindErrors name="allocateStudentsToTutorsCommand">
				<#if errors.hasErrors()>
					<div class="alert alert-error">
						<h3>Some problems need fixing</h3>
						<#if errors.hasGlobalErrors()>
							<#list errors.globalErrors as e>
								<div><@spring.message message=e /></div>
							</#list>
						<#else>
							<div>See the errors below.</div>
						</#if>
					</div>
				</#if>
			</@spring.hasBindErrors>

			<#assign submitUrl><@routes.tutors_allocate department /></#assign>
			<@f.form method="post" action="${submitUrl}" commandName="allocateStudentsToTutorsCommand" cssClass="form-horizontal">
			<div class="tabula-dnd" 
					 data-item-name="student" 
					 data-text-selector=".name h6"
					 data-use-handle="false"
					 data-selectables=".students .drag-target"
					 data-scroll="true"
					 data-remove-tooltip="Remove this student from this tutor">
				<div class="btn-toolbar">
					<a class="random btn" data-toggle="randomise" data-disabled-on="empty-list"
					   href="#" >
						<i class="icon-random"></i> Randomly allocate
					</a>
					<a class="return-items btn" data-toggle="return" data-disabled-on="no-allocation"
					   href="#" >
						<i class="icon-arrow-left"></i> Remove all
					</a>
				</div>
				<div class="row-fluid fix-on-scroll-container">
					<div class="span5">
						<div id="studentslist" 
								 class="students tabula-filtered-list"
								 data-item-selector=".student-list li">
							<h3>Students</h3>
							<div class="well ">
									<h4>Students with no personal tutor</h4>
									<#if features.personalTutorAssignmentFiltering>
										<div class="filter" id="filter-by-gender-controls">
											<select data-filter-attr="fGender">
												<option data-filter-value="*">Any Gender</option>
												<option data-filter-value="M">Male</option>
												<option data-filter-value="F">Female</option>
											</select>
										</div>
										<div class="filter" id="filter-by-year-controls">
											<select data-filter-attr="fYear">
												<option data-filter-value="*">Any Year of study</option>
												<#list allocateStudentsToTutorsCommand.allMembersYears as year>
													<option data-filter-value="${year}">Year ${year}</option>
												</#list>
											</select>
										</div>
										<div class="filter" id="filter-by-route-controls">
											<select data-filter-attr="fRoute">
												<option data-filter-value="*">Any Route</option>
												<#list allocateStudentsToTutorsCommand.allMembersRoutes as route>
													<option data-filter-value="${route.code}">${route.code?upper_case} ${route.name}</option>
												</#list>
											</select>
										</div>
									</#if>
								<div class="student-list drag-target">
									<ul class="drag-list return-list unstyled" data-bindpath="unallocated">
										<@spring.bind path="unallocated">
											<#list status.actualValue as student>
												<@student_item student "${status.expression}[${student_index}]" />
											</#list>
										</@spring.bind>
									</ul>
								</div>
							</div>
						</div>
					</div>
					<div class="span2">
						<#-- I, for one, welcome our new jumbo icon overlords -->
						<div class="direction-icon fix-on-scroll">
							<i class="icon-arrow-right"></i>
						</div>
					</div>
					<div class="span5">
						<div id="tutorslist" class="tutors fix-on-scroll">
							<button type="button" class="btn btn-primary pull-right" data-toggle="modal" data-target="#add-tutors">Add tutors</button>
							
							<#-- Modal to add students manually -->
							<div class="modal fade hide" id="add-tutors" tabindex="-1" role="dialog" aria-labelledby="add-tutors-label" aria-hidden="true">
								<div class="modal-header">
									<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
									<h3 id="add-tutors-label">Add tutors</h3>
								</div>
					
								<div class="modal-body">
									<p>
										Lookup tutors by typing their names, usercodes or university IDs below, then click <code>Add</code>.
									</p>
									
									<@form.labelled_row "additionalTutors" "Tutors">
										<@form.flexipicker path="additionalTutors" placeholder="User name" list=true multiple=true />
									</@form.labelled_row>
								</div>
					
								<div class="modal-footer">
									<button type="button" class="btn btn-primary refresh-form">Add</button>
								</div>
							</div>
						
							<h3>Tutors</h3>
							
							<#macro tutor_item university_id full_name existing_students=[]>
								<div class="drag-target well clearfix tutor-${university_id}">
									<div class="tutor-header">
										<a href="#" class="delete pull-right" data-toggle="tooltip" title="Remove ${full_name}">
											<i class="icon-remove icon-large"></i>
										</a>
									
										<#local popoverHeader>${full_name}'s personal tutees</#local>

										<h4 class="name">
											${full_name}
										</h4>

										<div>
											<#local count = existing_students?size />
											<span class="drag-count">${count}</span> <span class="drag-counted" data-singular="student" data-plural="students">student<#if count != 1>s</#if></span>

											<a id="show-list-${university_id}" class="show-list" title="View students" data-container=".tutor-${university_id}" data-title="${popoverHeader}" data-placement="left"><i class="icon-question-sign"></i></a>
										</div>
									</div>

									<ul class="drag-list hide" data-bindpath="mapping[${university_id}]">
										<#list existing_students as student>
											<@student_item student "mapping[${university_id}][${student_index}]" />
										</#list>
									</ul>
								</div>
							</#macro>
							
							<#list allocateStudentsToTutorsCommand.mapping?keys as tutor>
								<#assign existingStudents = mappingById[tutor.universityId]![] />
								
								<@tutor_item tutor.universityId tutor.fullName existingStudents />
							</#list>
						</div>
					</div>
				</div>
			</div>
			
				<#include "_allocate_notifications_modal.ftl" />

				<div class="submit-buttons">
					<button type="button" class="btn btn-primary" data-toggle="modal" data-target="#notify-modal">Save</button>
					<a href="<@routes.home />" class="btn">Cancel</a> <#-- TODO better url -->
				</div>
			</@f.form>
			</div><!-- end 1st tab -->

			<div class="tab-pane" id="allocatetutors-tab2">
				<#include "upload_form.ftl" />
			</div><!-- end 2nd tab-->

		</div><!-- end tab-content -->

	</div> <!-- end tabbable -->

	<script type="text/javascript">
		(function($) {
			<!--TAB-1008 - fix scrolling bug when student list is shorter than the group list-->
			$('#studentslist').css('min-height', function() {
				return $('#tutorslist').outerHeight();
			});
			
			$('.btn.refresh-form').on('click', function(e) {
				var $form = $(e.target).closest('form');
				$form.prepend($('<input />').attr({type: 'hidden', name: 'action', value: 'refresh'}));
				$form.submit();
			});
		})(jQuery);
	</script>

</#escape>