/* eslint-env browser */
import $ from 'jquery';

class SubmissionProgress {
  constructor(form) {
    this.form = form;

    $(form).on('submit', this.submissionAttempt.bind(this));
  }

  submissionAttempt(e) {
    const $form = $(this.form);
    const beacon = $form.data('beacon');
    if (beacon) {
      $.post({
        url: beacon,
        complete: () => this.submit(e),
      });
    } else {
      this.submit(e);
    }

    // Cancel the default form submission
    e.preventDefault();
    e.stopImmediatePropagation();
    return false;
  }

  submit() {
    // Replace the submit buttons with a progress bar
    const $form = $(this.form);
    this.$progress = $('<div />')
      .append(
        $('<p />').html('<i class="id7-koan-spinner id7-koan-spinner--xs id7-koan-spinner--inline" aria-hidden="true"></i> Uploading, please wait&hellip; ')
          .append($('<button />').addClass('btn btn-link').text('Cancel upload').on('click', this.cancel.bind(this))),
      )
      .append($('<div />').addClass('progress')
        .append($('<div />').addClass('progress-bar progress-bar-striped')
          .attr({
            role: 'progressbar',
            'aria-valuenow': 0,
            'aria-valuemin': 0,
            'aria-valuemax': 100,
          })));

    $form.find('.submit-buttons')
      .hide()
      .after(this.$progress);

    this.updateProgress(0);

    const formData = new FormData();

    $.each($form.serializeArray(), (i, obj) => formData.append(obj.name, obj.value));

    $form.find('input[type="file"]').each((i, el) => {
      const name = $(el).attr('name');
      $.each(el.files, (j, file) => {
        formData.append(name, file);
      });
    });

    const request = new XMLHttpRequest();
    this.request = request;

    let action = $form.attr('action');
    if (action.indexOf('#') !== -1) {
      action = action.substring(0, action.indexOf('#'));
    }

    request.open($form.attr('method') || 'POST', action, true);

    request.upload.addEventListener('progress', (evt) => {
      if (evt.lengthComputable) {
        const percentComplete = (evt.loaded / evt.total) * 100;
        this.updateProgress(percentComplete);
      }
    }, false);

    request.addEventListener('load', () => {
      let isSuccessful;

      // IE11 and early Edge don't support XHR.responseURL
      if ('responseURL' in request) {
        isSuccessful = request.responseURL.indexOf('justSubmitted') !== -1;
      } else {
        // Nasty, look for an alert-warning that's displayed when there's validation errors
        isSuccessful = $('.alert.alert-warning', request.responseText).length === 0;
      }

      if (isSuccessful) {
        this.uploadComplete();
      } else {
        // Validation error(s)
        this.$progress
          .find('.progress-bar')
          .removeClass('active')
          .addClass('progress-bar-danger');

        const id = $form.attr('id');
        const responseForm = $(`#${id}`, request.responseText);

        if (responseForm.length) {
          $form.html(responseForm.html());

          // Smooth scroll to the top of the form
          $('html, body').animate({
            scrollTop: $form.offset().top - 22, // @line-height-computed
          }, 300, 'swing', () => $('.headroom').headroom('unpin'));

          // Re-initialise the submission progress watcher
          init(); // eslint-disable-line no-use-before-define
        } else {
          // Maybe the deadline's passed or something. Either way, reload the whole page
          window.location.reload();
        }
      }
    }, false);

    request.addEventListener('abort', () => {
      // Remove the progress bar
      this.$progress.remove();

      // Re-enable the submit buttons
      $form.find('.submit-buttons').show()
        .find('.btn')
        .removeClass('disabled')
        .prop('disabled', false);
    }, false);

    request.addEventListener('error', () => {
      this.$progress
        .find('.progress-bar')
        .removeClass('active')
        .addClass('progress-bar-danger');

      // Re-enable the submit buttons
      $form.find('.submit-buttons').show()
        .find('.btn')
        .removeClass('disabled')
        .prop('disabled', false);
    }, false);

    request.send(formData);
  }

  cancel() {
    // Rely on the abort event listener
    this.request.abort();
  }

  updateProgress(p) {
    const percentage = Math.floor(p);
    const $bar = this.$progress.find('.progress-bar');

    $bar
      .toggleClass('active', p > 0)
      .attr('aria-valuenow', percentage)
      .css('width', `${Math.max(3, percentage)}%`)
      .empty();

    if (percentage === 100) {
      $bar.append($('<span />').html('Processing submission, please wait&hellip;'));
    } else {
      $bar.append($('<span />').addClass('sr-only').text(`${percentage}% Complete`));
    }
  }

  uploadComplete() {
    this.$progress
      .find('.progress-bar')
      .removeClass('active')
      .addClass('progress-bar-success')
      .attr('aria-valuenow', 100)
      .css('width', '100%')
      .empty()
      .append($('<span />').html('Processing submission, please wait&hellip;'));

    // Redirect to the responseURL
    if ('responseURL' in this.request) {
      window.location.assign(this.request.responseURL);
    } else {
      // IE11 doesn't support responseURL so guess at it
      window.location.assign(`${window.location.origin || ''}${window.location.pathname}?justSubmitted=true`);
    }
  }
}

function init() {
  $('#submitAssignmentCommand').each((i, el) => {
    $(el).data('tabula.submissionProgress', new SubmissionProgress(el));
  });
}

$(() => init());
