<#escape x as x?html>

  <h1>Download ${zipType} ZIP file</h1>

  <div class="alert alert-info">
    <p>The ZIP file is currently being generated. You can download it below when it is ready.</p>

    <div class="progress active">
      <div class="progress-bar progress-bar-striped" style="width: 0;"></div>
    </div>

    <p class="zip-progress">Initialising</p>
  </div>

  <div class="zip-complete alert alert-info" style="display: none;">
    <h3>ZIP file generated successfully</h3>
    <p><a href="<@routes.zipComplete jobId />" class="btn btn-link"><i class="icon-download fa fa-arrow-circle-o-down"></i> Download ZIP file</a></p>
  </div>

  <a class="btn btn-default" href="${returnTo}">Done</a>

  <script>
    jQuery(function ($) {
      var $refreshBtn = $('<button />')
        .addClass('btn btn-default btn-sm zip-progress')
        .attr({type: 'button'})
        .text('refresh the page')
        .on('click', function () {
          window.location.reload();
        });
      var $errorMessage = $('<p />')
        .append("We couldn't get the status of the zip file automatically. Please ")
        .append($refreshBtn)
        .append(" to see the latest status.");

      var updateProgress = function () {
        var $zipProgress = $('.zip-progress');
        var $progressBar = $('.progress .progress-bar');
        $.get('<@routes.zipProgress jobId />' + '?dt=' + new Date().valueOf(), function (data) {
          if (data.succeeded) {
            $progressBar.width("100%").removeClass('active progress-bar-striped');
            $zipProgress.empty();
            $('.zip-complete').show();
          } else {
            $progressBar.width(data.progress + "%");
            if (data.status) {
              $zipProgress.html(data.status);
            }
            setTimeout(updateProgress, 2 * 1000);
          }
        }).fail(function () {
          $progressBar.width("100%").addClass('progress-bar-danger').removeClass('active');
          $zipProgress.replaceWith($errorMessage);
        });
      };
      updateProgress();
    });
  </script>

</#escape>