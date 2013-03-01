<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>

<#macro assignStudents studentList markerList class name>
	<div class="btn-toolbar">
		<a class="random btn btn-mini"
		   href="#" >
			<i class="icon-random"></i> Randomly allocate
		</a>
		<a class="remove-all btn btn-mini"
		   href="#" >
			<i class="icon-arrow-left"></i> Remove all
		</a>
	</div>
	<div class="row-fluid">
		<div class="students span3">
			<h3>Students</h3>
			<div class="student-list">
				<ul class="member-list">
					<#list studentList as student>
						<li>
							<div class="student"
								 data-student-display="${student.displayValue}"
								 data-student-id="${student.userCode}">
								<i class="icon-user"></i> ${student.displayValue}
							</div>
						</li>
					</#list>
				</ul>
			</div>
		</div>
		<div class="${class} span9">
			<h3>${name}</h3>
			<ul class="member-list">
				<#list markerList as marker>
					<#assign existingStudents = marker.students />
					<li>
						<div class="marker" data-marker-id="${marker.userCode}">
							<span>${marker.fullName}</span>
							<span class="count badge badge-info">${existingStudents?size}</span>
							<div id="container-${marker.userCode}" class="student-container hidden">
								<ul class="student-list">
									<#list existingStudents as student>
										<li>
											${student.displayValue}
											<a class="remove-student btn btn-mini"
												href="#" data-marker-id="${marker.userCode}"
												data-student-id="${student.userCode}"
												data-student-display="${student.displayValue}">
												<i class="icon-remove"></i> Remove
											</a>
										</li>
									</#list>
								</ul>
								<#list existingStudents as student>
									<input type="hidden"
										   name="markerMapping[${marker.userCode}][${student_index}]"
										   value="${student.userCode}"
										   data-student-display="${student.displayValue}">
								</#list>
							</div>
							<a id="tool-tip-${marker.userCode}" class="btn btn-mini" data-toggle="button" href="#">
								<i class="icon-list"></i>
								List
							</a>
							<script type="text/javascript">
								jQuery(function($){
									$("#tool-tip-${marker.userCode}").popover({
										placement: 'right',
										html: true,
										content: function(){
											return $('<div />').append($('#container-${marker.userCode} .student-list').clone()).html();
										},
										title: 'Students to be marked by ${marker.fullName}'
									});
								});
							</script>
						</div>
					</li>
				</#list>
			</ul>
		</div>
	</div>
</#macro>


<#escape x as x?html>
	<h1>Assign markers for ${assignment.name}</h1>
	<@f.form method="post" action="${url('/admin/module/${module.code}/assignments/${assignment.id}/assign-markers')}" commandName="assignMarkersCommand">
	<div id="assign-markers">
		<ul class="nav nav-tabs">
			<li class="active">
				<a href="#first-markers">
					First markers
				</a>
			</li>
			<li>
				<a href="#second-markers">
					Second markers
				</a>
			</li>
		</ul>
		<div class="tab-content">
			<div class="tab-pane active" id="first-markers">
				<@assignStudents
					assignMarkersCommand.firstMarkerUnassignedStudents
					assignMarkersCommand.firstMarkers
					"first-markers"
					"First Markers"
				/>
			</div>
			<div class="tab-pane" id="second-markers">
				<@assignStudents
					assignMarkersCommand.secondMarkerUnassignedStudents
					assignMarkersCommand.secondMarkers
					"second-markers"
					"Second Markers"
				/>
			</div>
		</div>
	</div>
	<div class="submit-buttons">
		<input type="submit" class="btn btn-primary" value="Save">
		<a href="<@routes.depthome module />" class="btn">Cancel</a>
	</div>
	</@f.form>
</#escape>


