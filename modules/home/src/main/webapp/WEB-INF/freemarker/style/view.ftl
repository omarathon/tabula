<@stylesheet "/static/css/style-guide.css" />

<h2>Tabula style guide</h2>

<h3>Colour palette</h3>

<style type="text/css">
	.example { display: inline-block; width: 200px; height: 30px; text-align: center; line-height: 30px; }
</style>

<table class="table table-bordered table-striped">
	<thead>
		<tr>
			<th>class=""</th>
			<th>Use</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td><div class="example tabula-turquoise"><code>tabula-turquoise</code></div></td>
			<td>
				<ul>
					<li>Home module header colour</li>
					<li>Coursework management header colour</li>
					<li><code>&lt;h1&gt;</code></li>
				</ul>
			</td>
		</tr>
		<tr>
			<td><div class="example tabula-green"><code>tabula-green</code></div></td>
			<td>&nbsp;</td>
		</tr>
		<tr>
			<td><div class="example tabula-greenDark"><code>tabula-greenDark</code></div></td>
			<td>&nbsp;</td>
		</tr>
		<tr>
			<td><div class="example tabula-purple"><code>tabula-purple</code></div></td>
			<td>
				<ul>
					<li>Profiles module header colour</li>
					<li><code>&lt;h2&gt;</code> and <code>&lt;h6&gt;</code></li>
				</ul>
			</td>
		</tr>
		<tr>
			<td><div class="example tabula-pink"><code>tabula-pink</code></div></td>
			<td>&nbsp;</td>
		</tr>
		<tr>
			<td><div class="example tabula-greenLight"><code>tabula-greenLight</code></div></td>
			<td>&nbsp;</td>
		</tr>
		<tr>
			<td><div class="example tabula-yellowGreen"><code>tabula-yellowGreen</code></div></td>
			<td>
				<ul>
					<li><code>&lt;h4&gt;</code></li>
				</ul>
			</td>
		</tr>
		<tr>
			<td><div class="example tabula-blueLight"><code>tabula-blueLight</code></div></td>
			<td>
				<ul>
					<li>Colour on <code>btn-info</code></li>
				</ul>
			</td>
		</tr>
		<tr>
			<td><div class="example tabula-blueDark"><code>tabula-blueDark</code></div></td>
			<td>
				<ul>
					<li>Hyperlinks</li>
					<li><code>&lt;h3&gt;</code></li>
					<li>Accent colour on <code>btn-primary</code></li>
				</ul>
			</td>
		</tr>
		<tr>
			<td><div class="example tabula-blue"><code>tabula-blue</code></div></td>
			<td>&nbsp;</td>
		</tr>
		<tr>
			<td><div class="example tabula-orangeLight"><code>tabula-orangeLight</code></div></td>
			<td>&nbsp;</td>
		</tr>
		<tr>
			<td><div class="example tabula-orange"><code>tabula-orange</code></div></td>
			<td>
				<ul>
					<li>Scheduling module header colour</li>
				</ul>
			</td>
		</tr>
		<tr>
			<td><div class="example tabula-orangeDark"><code>tabula-orangeDark</code></div></td>
			<td>
				<ul>
					<li>"Warning" progress bar</li>
					<li>Text on "Warning" alerts</li>
				</ul>
			</td>
		</tr>
		<tr>
			<td><div class="example tabula-red"><code>tabula-red</code></div></td>
			<td>&nbsp;</td>
		</tr>
		<tr>
			<td><div class="example tabula-redDark"><code>tabula-redDark</code></div></td>
			<td>&nbsp;</td>
		</tr>
		<tr>
			<td><div class="example tabula-purpleDark"><code>tabula-purpleDark</code></div></td>
			<td>&nbsp;</td>
		</tr>
		<tr>
			<td><div class="example tabula-grayLight"><code>tabula-grayLight</code></div></td>
			<td>&nbsp;</td>
		</tr>
		<tr>
			<td><div class="example tabula-gray"><code>tabula-gray</code></div></td>
			<td>
				<ul>
					<li>Body text</li>
					<li><code>&lt;h5&gt;</code></li>
				</ul>
			</td>
		</tr>
		<tr>
			<td><div class="example tabula-grayDark"><code>tabula-grayDark</code></div></td>
			<td>&nbsp;</td>
		</tr>
	</tbody>
</table>

<h3>Buttons</h3>

<table class="table table-bordered table-striped">
    <thead>
      <tr>
        <th>Button</th>
        <th>class=""</th>
        <th>Description</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td><button type="button" class="btn">Default</button></td>
        <td><code>btn</code></td>
        <td>Use for secondary action in a set of buttons eg: Cancel</td>
      </tr>
      <tr>
        <td><button type="button" class="btn btn-primary">Primary</button></td>
        <td><code>btn btn-primary</code></td>
        <td>Use for primary action in a set of buttons eg: Add</td>
      </tr>
      <tr>
        <td><button type="button" class="btn btn-info">Info</button></td>
        <td><code>btn btn-info</code></td>
        <td>Use for a passive action such as toggling info on and off eg: Show all</td>
      </tr>
      <tr>
        <td><button type="button" class="btn btn-success">Success</button></td>
        <td><code>btn btn-success</code></td>
        <td>Use for a successful or positive action</td>
      </tr>
      <tr>
        <td><button type="button" class="btn btn-warning">Warning</button></td>
        <td><code>btn btn-warning</code></td>
        <td>Use for actions that should be taken with caution</td>
      </tr>
      <tr>
        <td><button type="button" class="btn btn-danger">Danger</button></td>
        <td><code>btn btn-danger</code></td>
        <td>Use for a dangerous or potentially negative action eg: Delete</td>
      </tr>
      <tr>
        <td><button type="button" class="btn btn-inverse">Inverse</button></td>
        <td><code>btn btn-inverse</code></td>
        <td>Alternate dark gray button, not tied to a semantic action or use</td>
      </tr>
      <tr>
        <td><button type="button" class="btn btn-link">Link</button></td>
        <td><code>btn btn-link</code></td>
        <td>Deemphasize a button by making it look like a link while maintaining button behavior</td>
      </tr>
    </tbody>
</table>

<h4>Examples</h4>

<p>
	<button class="btn btn-primary">Confirm</button> <a class="btn" href="#">Cancel</a>
</p>
	
<p>Wording, such as 'or', is not necessary between these two buttons. Primary action first, secondary action...err...second.</p>

<p>
	<button class="btn btn-danger">Delete</button>
</p>

<p>
	<button class="btn btn-info">Show all modules</button>
</p>

<p>We'd expect the Default, Primary, Info and Danger buttons to be used most often. Success, Warning and Inverse buttons to be used much more rarely and with caution!</p>

<h3>Labels</h3>

<table class="table table-bordered table-striped">
    <thead>
      <tr>
        <th>Labels</th>
        <th>Markup</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td>
          <span class="label">Default</span>
        </td>
        <td>
          <code>&lt;span class="label"&gt;Default&lt;/span&gt;</code>
        </td>
      </tr>
      <tr>
        <td>
          <span class="label label-success">Success</span>
        </td>
        <td>
          <code>&lt;span class="label label-success"&gt;Success&lt;/span&gt;</code>
        </td>
      </tr>
      <tr>
        <td>
          <span class="label label-warning">Warning</span>
        </td>
        <td>
          <code>&lt;span class="label label-warning"&gt;Warning&lt;/span&gt;</code>
        </td>
      </tr>
      <tr>
        <td>
          <span class="label label-important">Important</span>
        </td>
        <td>
          <code>&lt;span class="label label-important"&gt;Important&lt;/span&gt;</code>
        </td>
      </tr>
      <tr>
        <td>
          <span class="label label-info">Info</span>
        </td>
        <td>
          <code>&lt;span class="label label-info"&gt;Info&lt;/span&gt;</code>
        </td>
      </tr>
      <tr>
        <td>
          <span class="label label-inverse">Inverse</span>
        </td>
        <td>
          <code>&lt;span class="label label-inverse"&gt;Inverse&lt;/span&gt;</code>
        </td>
      </tr>
    </tbody>
</table>

<h3>Badges</h3>

<table class="table table-bordered table-striped">
    <thead>
      <tr>
        <th>Name</th>
        <th>Example</th>
        <th>Markup</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td>
          Default
        </td>
        <td>
          <span class="badge">1</span>
        </td>
        <td>
          <code>&lt;span class="badge"&gt;1&lt;/span&gt;</code>
        </td>
      </tr>
      <tr>
        <td>
          Success
        </td>
        <td>
          <span class="badge badge-success">2</span>
        </td>
        <td>
          <code>&lt;span class="badge badge-success"&gt;2&lt;/span&gt;</code>
        </td>
      </tr>
      <tr>
        <td>
          Warning
        </td>
        <td>
          <span class="badge badge-warning">4</span>
        </td>
        <td>
          <code>&lt;span class="badge badge-warning"&gt;4&lt;/span&gt;</code>
        </td>
      </tr>
      <tr>
        <td>
          Important
        </td>
        <td>
          <span class="badge badge-important">6</span>
        </td>
        <td>
          <code>&lt;span class="badge badge-important"&gt;6&lt;/span&gt;</code>
        </td>
      </tr>
      <tr>
        <td>
          Info
        </td>
        <td>
          <span class="badge badge-info">8</span>
        </td>
        <td>
          <code>&lt;span class="badge badge-info"&gt;8&lt;/span&gt;</code>
        </td>
      </tr>
      <tr>
        <td>
          Inverse
        </td>
        <td>
          <span class="badge badge-inverse">10</span>
        </td>
        <td>
          <code>&lt;span class="badge badge-inverse"&gt;10&lt;/span&gt;</code>
        </td>
      </tr>
    </tbody>
</table>

<h3>Alerts</h3>

<div class="alert">
  <button type="button" class="close" data-dismiss="alert">×</button>
  <strong>Warning!</strong> Best check yo self, you're not looking too good.
</div>

<div class="alert alert-error">
  <button type="button" class="close" data-dismiss="alert">×</button>
  <strong>Oh snap!</strong> Change a few things up and try submitting again.
</div>
            
<div class="alert alert-success">
  <button type="button" class="close" data-dismiss="alert">×</button>
  <strong>Well done!</strong> You successfully read this important alert message.
</div>

<div class="alert alert-info">
  <button type="button" class="close" data-dismiss="alert">×</button>
  <strong>Heads up!</strong> This alert needs your attention, but it's not super important.
</div>