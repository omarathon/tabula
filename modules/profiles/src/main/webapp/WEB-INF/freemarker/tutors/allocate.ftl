<#assign department=allocateStudentsToTutorsCommand.department />
<#assign mappingById=allocateStudentsToTutorsCommand.mappingById />
<#assign membersById=allocateStudentsToTutorsCommand.membersById />



<#macro student_item profile bindpath="">
	<li class="student well well-small"
	data-f-gender="${profile.gender.dbValue}"
	data-f-route="${profile.mostSignificantCourseDetails.route.code}"
	data-f-year="${profile.mostSignificantCourseDetails.latestStudentCourseYearDetails.yearOfStudy}">
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
			<div class="tab-pane active" id="allocatetutors-tab1">

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
			<@f.form method="post" action="${submitUrl}" commandName="allocateStudentsToTutorsCommand">
				<div class="btn-toolbar">
					<a class="random btn"
					   href="#" >
						<i class="icon-random"></i> Randomly allocate
					</a>
					<a class="return-items btn"
					   href="#" >
						<i class="icon-arrow-left"></i> Remove all
					</a>
				</div>
				<div class="row-fluid fix-on-scroll-container">
					<div class="span5">
						<div id="studentslist" class="students">
							<h3>Students</h3>
							<div class="well ">
									<h4>Students with no personal tutor</h4>
									<#if features.personalTutorAssignmentFiltering>
										<div id="filter-controls">
										Show...
										<div id="filter-by-gender-controls">
										<label>Male <input type="checkbox" data-filter-attr="fGender" data-filter-value="M" checked="checked"></label>
										<label>Female <input type="checkbox" data-filter-attr="fGender" data-filter-value="F" checked="checked"></label>
										</div>
										<div id="filter-by-year-controls">
										<#list allocateStudentsToTutorsCommand.allMembersYears as year>
											<label>Year ${year} <input type="checkbox" data-filter-attr="fYear" data-filter-value="${year}" checked="checked"></label>
										</#list>
										</div>
										<div id="filter-by-route-controls">
										<#list allocateStudentsToTutorsCommand.allMembersRoutes as route>
											<label>${route.name} <input type="checkbox" data-filter-attr="fRoute" data-filter-value="${route.code}" checked="checked"></label>
										</#list>
										</div>
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
							<button type="button" class="btn btn-primary pull-right">Add tutors</button>
						
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
							
							<#-- For adding new tutors -->
							<script id="tutor-template" type="text/template">
								<@tutor_item "{{university_id}}" "{{full_name}}" />
							</script>
						</div>
					</div>
				</div>

				<div class="submit-buttons">
					<input type="submit" class="btn btn-primary" value="Save">
					<a href="<@routes.home />" class="btn">Cancel</a> <#-- TODO better url -->
				</div>
			</@f.form>
			</div><!-- end 1st tab -->

			<div class="tab-pane" id="allocatetutors-tab2">
<#--
				<@f.form method="post" enctype="multipart/form-data" action="${submitUrl}" commandName="allocateStudentsToTutorsCommand">

					<p>You can allocate students to groups using a spreadsheet.</p>

					<ol>
						<li><strong><a href="allocate/template">Download a template spreadsheet</a></strong>. This will be prefilled with the names and University ID numbers of students you have selected to be in Term 1 seminars. In Excel you may need to <a href="http://office.microsoft.com/en-gb/excel-help/what-is-protected-view-RZ101665538.aspx?CTT=1&section=7">exit protected view</a> to edit the spreadsheet.
						</li>
						<li><strong>Allocate students</strong> to groups using the dropdown menu in the <strong>Group name</strong> column or by pasting in a list of group names. The group names must match the groups you have already created for Term 1 seminars. The <strong>group_id</strong> field will be updated with a unique ID number for that group.
							You can select additional students to be in Term 1 seminars by entering their University ID numbers in the <strong>student_id</strong> column. Any students with an empty group_id field will be added to the list of students who haven't been allocated to a group.</li>
						<li><strong>Save</strong> your updated spreadsheet.</li>
					    <li><@form.labelled_row "file.upload" "Choose your updated spreadsheet" "step-action" ><input type="file" name="file.upload"  /> </@form.labelled_row></li>
					</ol>

					<input name="isfile" value="true" type="hidden"/>


					<div class="submit-buttons">
						<button class="btn btn-primary btn-large"><i class="icon-upload icon-white"></i> Upload</button>
					</div>
				</@f.form>
-->
			</div><!-- end 2nd tab-->

		</div><!-- end tab-content -->

	</div> <!-- end tabbable -->

	<script type="text/javascript">
		(function($) {
			<!--TAB-1008 - fix scrolling bug when student list is shorter than the group list-->
			$('#studentslist').css('min-height', function() {
				return $('#groupslist').outerHeight();
			});
		})(jQuery);
	</script>

</#escape>