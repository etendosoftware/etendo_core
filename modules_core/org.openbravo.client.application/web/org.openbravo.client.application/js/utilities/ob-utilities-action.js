/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2012-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

OB = window.OB || {};
OB.Utilities = window.OB.Utilities || {};

// = Openbravo Action Utilities =
// Defines utility methods to execute functions based in a Json argument.
OB.Utilities.Action = {
  _list: [],
  _pausedThreads: [],
  _cancelledThreads: [],

  // ** {{{ pauseThread(/*String*/ threadId) }}} **
  // Pauses the thread with id passed as "threadId" argument
  // Parameters:
  // * {{{threadId}}}: the id of the thread to pause
  // * {{{delay}}}: delay in ms to pause the thread
  pauseThread: function(threadId, delay) {
    var position = this._pausedThreads.length,
      i;

    if (delay && Object.prototype.toString.apply(delay) === '[object Number]') {
      setTimeout(function() {
        OB.Utilities.Action.pauseThread(threadId);
      }, delay);
      return true;
    }
    for (i = 0; i < position; i++) {
      if (this._pausedThreads[i] === threadId) {
        position = i;
        break;
      }
    }
    this._pausedThreads[position] = threadId;
    return true;
  },

  // ** {{{ resumeThread(/*String*/ threadId) }}} **
  // Resumes the thread with id passed as "threadId" argument
  // Parameters:
  // * {{{threadId}}}: the id of the thread to resume
  // * {{{delay}}}: delay in ms to resume the thread
  resumeThread: function(threadId, delay) {
    var position = null,
      i;

    if (delay && Object.prototype.toString.apply(delay) === '[object Number]') {
      setTimeout(function() {
        OB.Utilities.Action.resumeThread(threadId);
      }, delay);
      return true;
    }
    for (i = 0; i < this._pausedThreads.length; i++) {
      if (this._pausedThreads[i] === threadId) {
        position = i;
        break;
      }
    }
    if (position !== null) {
      this._pausedThreads.splice(position, 1);
      return true;
    } else {
      return false;
    }
  },

  // ** {{{ isThreadPaused(/*String*/ threadId) }}} **
  // Returns true if the thread with id passed as "threadId" argument is paused.
  // Else it returns false.
  // Parameters:
  // * {{{threadId}}}: the id of the thread to check
  isThreadPaused: function(threadId) {
    var position = this._pausedThreads.length,
      i;

    for (i = 0; i < position; i++) {
      if (this._pausedThreads[i] === threadId) {
        return true;
      }
    }
    return false;
  },

  // ** {{{ cancelThread(/*String*/ threadId) }}} **
  // Cancels the thread with id passed as "threadId" argument
  // Parameters:
  // * {{{threadId}}}: the id of the thread to cancel
  // * {{{delay}}}: delay in ms to cancel the thread
  cancelThread: function(threadId, delay) {
    var position = this._cancelledThreads.length;

    if (delay && Object.prototype.toString.apply(delay) === '[object Number]') {
      setTimeout(function() {
        OB.Utilities.Action.cancelThread(threadId);
      }, delay);
      return true;
    }
    this._cancelledThreads[position] = threadId;
    if (this.isThreadPaused(threadId)) {
      this.resumeThread(threadId);
    }
    return true;
  },

  // ** {{{ isThreadCancelled(/*String*/ threadId) }}} **
  // Returns true if the thread with id passed as "threadId" argument is cancelled.
  // Else it returns false.
  // Parameters:
  // * {{{threadId}}}: the id of the thread to check
  isThreadCancelled: function(threadId) {
    var position = this._cancelledThreads.length,
      i;

    for (i = 0; i < position; i++) {
      if (this._cancelledThreads[i] === threadId) {
        return true;
      }
    }
    return false;
  },

  // ** {{{ set(/*String*/ name, /*Function*/ action) }}} **
  // Registers a new function. After it, it will be available to access it using execute/executeJSON
  // The action (/*Function*/) only should accept one argument, and it should be a JSON object
  // Parameters:
  // * {{{name}}}: name of the function
  // * {{{action}}}: function associated to "name"
  set: function(name, action) {
    var position = this._list.length,
      i;

    for (i = 0; i < position; i++) {
      if (this._list[i].name === name) {
        position = i;
        break;
      }
    }
    this._list[position] = {};
    this._list[position].name = name;
    this._list[position].action = action;
    return true;
  },

  // ** {{{ remove(/*String*/ name) }}} **
  // Removes a new function. After it, it won't be avallable anymore to access it using execute/executeJSON
  // Parameters:
  // * {{{name}}}: name of the function
  remove: function(name) {
    var position = null,
      i;

    for (i = 0; i < this._list.length; i++) {
      if (this._list[i].name === name) {
        position = i;
        break;
      }
    }
    if (position !== null) {
      this._list.splice(position, 1);
      return true;
    } else {
      return false;
    }
  },

  // ** {{{ execute(/*String*/ name, /*Object*/ paramObj) }}} **
  // Executes the defined function with given "name" (if it is set/defined) and passing as argument the "paramObj" object
  // Parameters:
  // * {{{name}}}: name of the function
  // * {{{paramObj}}}: object passed as parameter to the function
  // * {{{delay}}}: delay in ms to start the action execution
  execute: function(name, paramObj, delay) {
    var length = this._list.length,
      i;

    if (delay && Object.prototype.toString.apply(delay) === '[object Number]') {
      setTimeout(function() {
        OB.Utilities.Action.execute(name, paramObj);
      }, delay);
      return true;
    }
    for (i = 0; i < length; i++) {
      if (
        this._list[i].name === name &&
        this._list[i].action !== null &&
        typeof this._list[i].action === 'function'
      ) {
        return this._list[i].action(paramObj);
      }
    }
    return false;
  },

  // ** {{{ execute(/*Object*/ or /*Array*/ jsonArray, /*String*/ threadId) }}} **
  // It executes a sequence of functions determined by the "jsonArray"
  // "jsonArray" could be a standalone object or an array of objects.
  // The object should have this structure
  //   { functionName : { attributeA: valueA, attributeB: valueB, .. } }
  // And for each object something like that will be executed sequentially:
  //   this.execute(functionName, { attributeA: valueA, attributeB: valueB, .. })
  // Parameters:
  // * {{{jsonArray}}}: object or array of objects to the passed
  // * {{{threadId}}}: the Id of the execution thread. If empty, a random one will be generated
  // * {{{delay}}}: delay in ms to start the action execution
  // * {{{processView}}}: view of the process that invoked the execution
  executeJSON: function(jsonArray, threadId, delay, processView) {
    var length = jsonArray.length,
      object,
      member,
      paramObj;

    if (delay && Object.prototype.toString.apply(delay) === '[object Number]') {
      setTimeout(function() {
        OB.Utilities.Action.executeJSON(jsonArray, threadId, null, processView);
      }, delay);
      return true;
    }
    if (Object.prototype.toString.apply(jsonArray) === '[object Object]') {
      jsonArray = [jsonArray];
    }
    if (Object.prototype.toString.apply(jsonArray) !== '[object Array]') {
      return false;
    }
    if (this.isThreadCancelled(threadId)) {
      return false;
    } else if (!threadId) {
      threadId = OB.Utilities.generateRandomString(8, true, true, true, false);
    } else if (this.isThreadPaused(threadId)) {
      this.executeJSON(jsonArray, threadId, 100, processView); //Call this action again with a 100ms delay
      return true;
    }

    object = jsonArray[0];
    if (Object.prototype.toString.apply(object) === '[object Object]') {
      for (member in object) {
        if (Object.prototype.hasOwnProperty.call(object, member)) {
          if (
            Object.prototype.toString.apply(object[member]) ===
            '[object Object]'
          ) {
            object[member].threadId = threadId;
            paramObj = object[member];
            if (paramObj) {
              paramObj._processView = processView;
            }
            this.execute(member, paramObj);
          }
        }
      }
    }
    if (length > 1) {
      jsonArray.splice(0, 1);
      return this.executeJSON(jsonArray, threadId, null, processView);
    } else {
      return true;
    }
  }
};
