/* eslint-env browser */
import $ from 'jquery';

export default class CsrfForm {
  static generate() {
    const $meta = $('meta[name=_csrf]');
    let csrfTokenValue = '';
    if ($meta.length > 0 && $meta.attr('content') !== undefined) {
      csrfTokenValue = $meta.attr('content');
    }
    const $form = $('<form>').attr('method', 'POST');
    $form.append($('<input>').attr('type', 'hidden').attr('value', csrfTokenValue).attr('name', 'urn:websignon:csrf'));
    return $form;
  }
}
