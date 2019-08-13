/* eslint-env browser */
import $ from 'jquery';

export default class CsrfForm {
  // get csrf value from current page
  static getCsrfValue() {
    const $meta = $('meta[name=_csrf]');
    if ($meta.length > 0 && $meta.attr('content') !== undefined) return $meta.attr('content');
    return '';
  }

  // create a hidden csrf field
  static makeCsrfField() {
    return $('<input>').attr('type', 'hidden').attr('value', CsrfForm.getCsrfValue()).attr('name', 'urn:websignon:csrf');
  }

  // append a hidden csrf field to an existing $form
  static appendCsrfField($form) {
    return $form.append(CsrfForm.makeCsrfField());
  }

  // generate a new empty form with hidden csrf field
  static generate() {
    return CsrfForm.appendCsrfField($('<form>').attr('method', 'POST'));
  }
}
