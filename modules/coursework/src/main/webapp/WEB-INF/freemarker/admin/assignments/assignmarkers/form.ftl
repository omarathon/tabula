<#macro student_item student bindpath="">
<li class="student well well-small ui-draggable ui-selectee">
	<div class="name"><h6><i class="icon-white icon-user"></i> ${student.displayValue}</h6></div>
	<input type="hidden" name="${bindpath}" value="${student.userCode}" />
</li>
</#macro>

<#macro assignStudents studentList markerList class name markerMapName>
<div class="tabula-dnd" data-scroll="true">
	<p>Drag students onto a marker to allocate them. Select multiple students by dragging a box around them.
		You can also hold the <kbd class="keyboard-control-key">Ctrl</kbd> key and drag to add to a selection.</p>

	<div class="tabula-dnd"
		 data-item-name="student"
		 data-text-selector=".name h6"
		 data-selectables=".students .drag-target"
		 data-scroll="true"
		 data-remove-tooltip="Unassign this student from the marker">

		<div class="fix-header pad-when-fixed">
			<div class="btn-toolbar">
				<a class="random btn btn-mini" data-toggle="randomise" data-disabled-on="empty-list"
				   href="#" >
					<i class="icon-random"></i> Randomly allocate
				</a>
				<a class="return-items btn btn-mini" data-toggle="return" data-disabled-on="no-allocation"
				   href="#" >
					<i class="icon-arrow-left"></i> Remove all
				</a>
			</div>
			<div class="row-fluid hide-smallscreen">
				<div class="span5">
					<h3>Students</h3>
				</div>
				<div class="span2"></div>
				<div class="span5">
					<h3>${name}</h3>
				</div>
			</div>
		</div><!-- end persist header -->

		<div class="row-fluid fix-on-scroll-container">
			<div class="span5">
				<div id="studentslist"
			 		 class="students tabula-filtered-list"
					 data-item-selector=".student-list li">
					<div class="well ">
						<h4>Not allocated to a marker</h4>
						<div class="student-list drag-target">
							<ul class="drag-list return-list unstyled" data-nobind="true">
								<#list studentList as student>
									<@student_item student "" />
								</#list>
							</ul>
						</div>
					</div>
				</div>
			</div>
			<div class="span2">
				<div class="direction-icon fix-on-scroll hide-smallscreen">
					<i class="icon-arrow-right"></i>
				</div>
			</div>
			<div class="span5">
				<h3 class="smallscreen-only">Groups</h3>

				<div id="groupslist" class="agentslist ${class} fix-on-scroll">
					<#list markerList as marker>
						<#local existingStudents = marker.students />

						<div class="marker drag-target agent-${markerMapName}-${marker.userCode}">
							<div class="group-header">
								<h4 class="name">
									${marker.fullName}
								</h4>
								<div>
									<#assign count = existingStudents?size />
									<span class="drag-count">${count}</span>
									<span class="drag-counted" data-singular="student" data-plural="students">
										student<#if count != 1>s</#if>
									</span>
									<a id="show-list-${markerMapName}-${marker.userCode}"
									   class="show-list"
									   title="View students"
									   data-container=".agent-${markerMapName}-${marker.userCode}"
									   data-title="Students assigned to ${marker.fullName}"
									   data-placement="left">
										<i class="icon-edit"></i>
									</a>
								</div>
							</div>
							<ul class="drag-list hide" data-bindpath="${markerMapName}[${marker.userCode}]">
								<#list existingStudents as student>
									<@student_item student "${markerMapName}[${marker.userCode}][${student_index}]" />
								</#list>
							</ul>
						</div>
					</#list>
				</div>
			</div>
		</div>

	</div>
</div>
<div class="submit-buttons">
	<input type="submit" name="dragAndDrop" class="btn btn-primary" value="Save">
	<a href="<@routes.depthome module />" class="btn">Cancel</a>
</div>
</#macro>


<#escape x as x?html>

	<h1>Assign markers</h1>
	<h4><span class="muted">for</span>
	${assignment.name}</h4>

		<@f.form method="post" enctype="multipart/form-data" action="${url('/coursework/admin/module/${module.code}/assignments/${assignment.id}/assign-markers')}" commandName="command">
		<div class="fix-area">
			<div id="assign-markers">
				<ul class="nav nav-tabs">
					<li class="active"><a href="#first-markers">${firstMarkerRoleName}s</a></li>
					<#if hasSecondMarker><li><a href="#second-markers">${secondMarkerRoleName}s</a></li></#if>
					<li><a href="#spreadsheet">Upload spreadsheet</a></li>
				</ul>

				<div class="tab-content">
					<div class="tab-pane active" id="first-markers">
						<@assignStudents
							firstMarkerUnassignedStudents
							firstMarkers
							"first-markers"
							firstMarkerRoleName
							"firstMarkerMapping"
						/>
					</div>
					<#if hasSecondMarker>
						<div class="tab-pane" id="second-markers">
							<@assignStudents
								secondMarkerUnassignedStudents
								secondMarkers
								"second-markers"
								secondMarkerRoleName
								"secondMarkerMapping"
							/>
						</div>
					</#if>
					<div class="tab-pane" id="spreadsheet">
						<p>You can assign students to markers by uploading a spreadsheet.</p>
						<ol>
							<li>
								<p><a class="btn" href="assign-markers/template">
									<i class="icon-download"></i> Download a template spreadsheet
								</a></p>
								<div class="alert alert-info">
									Any markers that you have already assigned using the drag and drop interface will be present in the template.
								</div>
							</li>
							<li>
								<p>
									Allocate students to markers using the dropdown menu in the marker name column or by typing a personal tutor's University ID into the agent_id column. The agent_id field will be updated with the University ID for that personal tutor if you use the dropdown. Any students with an empty agent_id field will have their marker removed, if they have one.
								</p>
							</li>
							<li><p><strong>Save</strong> your updated spreadsheet.</p></li>
							<li>
								<@form.labelled_row "file.upload" "Choose your updated spreadsheet" "step-action" >
									<input type="file" name="file.upload"  />
								</@form.labelled_row>
							</li>
						</ol>
						<div class="fix-footer submit-buttons">
							<input type="submit" name="uploadSpreadsheet" class="btn btn-primary" value="Upload">
							<a href="<@routes.depthome module />" class="btn">Cancel</a>
						</div>
					</div>
				</div>
			</div>


		</div><!-- end fix-area -->
	</@f.form>

<script type="text/javascript">
(function($) {
	var fixHeaderFooter = $('.fix-area').fixHeaderFooter();
	var singleColumnDragTargetHeight = $('.agentslist .drag-target').outerHeight(true);

	$(window).scroll(function() {
		fixHeaderFooter.fixTargetList('.agentslist:visible');
	});

	// when tabs and fixed headers collide
	var $tabs = $('.nav');
	var $tabFixedHeaders = $tabs.siblings('.tab-content').find('.fix-header');

	$tabs.on('shown', function() {
		$tabFixedHeaders.trigger('disabled.ScrollToFixed');
		$tabFixedHeaders.filter(':visible').trigger('enable.ScrollToFixed');
		$(window).resize();
	});

	// onload reformat agents layout
	formatAgentsLayout();

	// debounced on window resize, reformat agents layout...
	on_resize(formatAgentsLayout);


	function formatAgentsLayout() {
		var agentslist = $('.agentslist:visible');
		var heightOfSingleColumnList = agentslist.find('.drag-target').length * singleColumnDragTargetHeight;

		if(agentslist.height() > fixHeaderFooter.viewableArea()) {
			agentslist.addClass('drag-list-three-col');
		} else if(fixHeaderFooter.viewableArea() > heightOfSingleColumnList) {
			agentslist.removeClass('drag-list-three-col');
		}
	}

	// debounced on_resize to avoid retriggering while resizing window
	function on_resize(c,t) {
		onresize = function() {
			clearTimeout(t);
			t = setTimeout(c,100)
		};
		return c;
	}

})(jQuery);
</script>

</#escape>


