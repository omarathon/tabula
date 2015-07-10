<div class="row-fluid">
	<div class="span6">
		<h2>Search for individual students</h2>
		<div class="flexi-picker-container input-prepend input-append">
			<span class="add-on"><i class="icon-user fa fa-user"></i></span><#--
				--><input type="text" class="text flexi-picker"
						  name="singleSearch" placeholder="Enter name"
						  data-include-users="true" data-include-email="false" data-include-groups="false"
						  data-members-only="true" data-type="" autocomplete="off"
				/>
			<button class="btn" type="submit" name="searchSingle" value="true"><i class="icon-search fa fa-search"></i></button>
		</div>

	</div>
	<div class="span6">
		<h2>Search for multiple Student IDs</h2>
		<textarea name="multiSearch" rows="5" style="width:300px;" placeholder="Add one ID number per line">${command.multiSearch!}</textarea>
		<button class="btn" type="submit" name="searchMulti" value="true" style="vertical-align: bottom"><i class="icon-search fa fa-search"></i></button>
	</div>
</div>
<hr />