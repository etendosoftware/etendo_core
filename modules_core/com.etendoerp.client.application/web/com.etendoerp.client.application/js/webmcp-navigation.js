/*
 * WebMCP navigation tools for Etendo.
 *
 * Exposes the application menu and the open-tab set to in-browser AI agents
 * through the W3C WebMCP API (navigator.modelContext). Read-only + navigation
 * only: nothing here creates, updates or deletes business data.
 *
 * No-ops when the browser does not implement navigator.modelContext.
 */
(function(OB, isc) {
  // No 'use strict' on purpose: SmartClient reads arguments.caller/callee inside
  // its own methods, and calling them from strict-mode code throws a TypeError.

  if (!OB || !isc) {
    return;
  }

  var MAX_RESULTS = 50;

  OB.WebMCP = OB.WebMCP || {};

  // Flattens OB.Application.menu into a list of navigable leaf entries.
  function flattenMenu(nodes, path, out) {
    var i, node, currentPath;
    if (!nodes) {
      return out;
    }
    for (i = 0; i < nodes.length; i++) {
      node = nodes[i];
      currentPath = path ? path + ' / ' + node.title : node.title;
      if (node.submenu && node.submenu.length > 0) {
        flattenMenu(node.submenu, currentPath, out);
      } else if (node.windowId || node.viewId || node.processId) {
        out.push({
          title: node.title,
          path: currentPath,
          kind: node.type,
          windowId: node.windowId || null,
          tabId: node.tabId || null,
          viewId: node.viewId || null,
          menuEntryId: node.id || null
        });
      }
    }
    return out;
  }

  OB.WebMCP.getMenuEntries = function() {
    return flattenMenu(OB.Application.menu, '', []);
  };

  function matches(entry, query) {
    var q = query.toLowerCase();
    return (
      (entry.title || '').toLowerCase().indexOf(q) !== -1 ||
      (entry.path || '').toLowerCase().indexOf(q) !== -1
    );
  }

  OB.WebMCP.findMenuEntries = function(query, kind, limit) {
    var entries = OB.WebMCP.getMenuEntries();
    if (kind) {
      entries = entries.filter(function(e) {
        return e.kind === kind;
      });
    }
    if (query) {
      entries = entries.filter(function(e) {
        return matches(e, query);
      });
    }
    return entries.slice(0, limit || MAX_RESULTS);
  };

  // Resolves a loose reference (windowId, tabId or title) to a menu entry.
  OB.WebMCP.resolveEntry = function(ref) {
    var entries = OB.WebMCP.getMenuEntries(),
      exact;
    if (ref.windowId) {
      exact = entries.filter(function(e) {
        return e.windowId === ref.windowId;
      });
    } else if (ref.tabId) {
      exact = entries.filter(function(e) {
        return e.tabId === ref.tabId;
      });
    } else if (ref.title) {
      exact = entries.filter(function(e) {
        return (e.title || '').toLowerCase() === ref.title.toLowerCase();
      });
      if (exact.length === 0) {
        exact = OB.WebMCP.findMenuEntries(ref.title, null, 5);
      }
    } else {
      exact = [];
    }
    return exact;
  };

  OB.WebMCP.listOpenTabs = function() {
    var tabs = (OB.MainView && OB.MainView.TabSet && OB.MainView.TabSet.tabs) || [],
      selected =
        OB.MainView && OB.MainView.TabSet
          ? OB.MainView.TabSet.getSelectedTab()
          : null;
    return tabs.map(function(tab) {
      return {
        viewTabId: tab.pane ? tab.pane.viewTabId : null,
        title: isc.isA.String(tab.title)
          ? tab.title.replace(/<[^>]*>/g, '').trim()
          : null,
        windowId: tab.pane ? tab.pane.windowId || null : null,
        active: !!selected && selected.ID === tab.ID
      };
    });
  };


  // Resolves the active view of a tab (the standard window's currently focused
  // tab level), or null when the tab is not a standard window -- the Workspace,
  // process definitions and external pages have no grid.
  OB.WebMCP.getActiveView = function(viewTabId) {
    var tabSet = OB.MainView && OB.MainView.TabSet,
      tabs,
      tab = null,
      i,
      pane;
    if (!tabSet) {
      return null;
    }
    tabs = tabSet.tabs || [];
    if (viewTabId) {
      for (i = 0; i < tabs.length; i++) {
        if (tabs[i].pane && tabs[i].pane.viewTabId === viewTabId) {
          tab = tabs[i];
          break;
        }
      }
    } else {
      tab = tabSet.getSelectedTab();
    }
    if (!tab || !tab.pane) {
      return null;
    }
    pane = tab.pane;
    // A standard window exposes activeView; fall back to its root view.
    return pane.activeView || pane.view || null;
  };


  // --- Record editing helpers ----------------------------------------------

  // Writes that change or commit data ask the user first. Set to false only for
  // demos or automated runs; the agent itself must never flip it.
  OB.WebMCP.requireConfirmation = true;

  function askUser(message) {
    return new Promise(function(resolve) {
      if (!OB.WebMCP.requireConfirmation) {
        resolve(true);
        return;
      }
      isc.ask(message, function(ok) {
        resolve(!!ok);
      });
    });
  }

  // The form currently being edited, or null when the view is showing its grid.
  function getEditForm(viewTabId) {
    var view = OB.WebMCP.getActiveView(viewTabId);
    if (!view) {
      return null;
    }
    if (view.isShowingForm && view.viewForm) {
      return view.viewForm;
    }
    if (view.viewGrid && view.viewGrid.getEditForm) {
      return view.viewGrid.getEditForm() || null;
    }
    return null;
  }

  function describeField(item) {
    var value = null;
    try {
      value = item.getValue();
    } catch (e) {
      value = null;
    }
    return {
      name: item.name,
      label: item.title || item.name,
      type: item.type || null,
      required: !!item.required,
      // Only items backed by a column are real data fields. The form also holds
      // section headers and canvases (Audit, Notes, Attachments) which look like
      // fields but write nowhere.
      isDataField: !!item.inpColumnName,
      readOnly: !!(item.disabled || item.readOnly),
      // Reference fields need the record id as value, not the display text.
      isReference: !!item.valueMap || !!item.displayField,
      value: value === undefined ? null : value
    };
  }

  // Waits for a callout round trip (FIC) to settle, so dependent fields are
  // recalculated before the next write or the save.
  function waitForCallouts(form, timeoutMs) {
    var deadline = Date.now() + (timeoutMs || 8000);
    return new Promise(function(resolve) {
      (function poll() {
        if (!form.inFicCall || Date.now() > deadline) {
          resolve(!form.inFicCall);
          return;
        }
        setTimeout(poll, 150);
      })();
    });
  }

  // Toolbar buttons plus the process buttons a window adds on the right.
  function collectButtons(view) {
    var buttons = [],
      bar = view && view.toolBar;
    if (!bar) {
      return buttons;
    }
    (bar.leftMembers || []).forEach(function(b) {
      if (b && b.buttonType) {
        buttons.push({
          id: b.buttonType,
          label: b.prompt || b.buttonType,
          kind: 'toolbar',
          disabled: !!b.isDisabled(),
          button: b
        });
      }
    });
    (bar.rightMembers || []).forEach(function(b) {
      if (b && (b.property || b.title)) {
        buttons.push({
          id: b.property || b.title,
          label: b.title || b.property,
          kind: 'process',
          disabled: !!(b.isDisabled && b.isDisabled()),
          button: b
        });
      }
    });
    return buttons;
  }

  // --- WebMCP registration -------------------------------------------------

  var TOOLS = [
    {
      name: 'etendo_list_windows',
      description:
        'List the Etendo ERP windows, reports and processes the current user can open, ' +
        'as they appear in the application menu. Use it to discover what is available ' +
        'and to obtain the windowId/tabId needed by etendo_open_window. ' +
        'Returns menu path, title and identifiers.',
      inputSchema: {
        type: 'object',
        properties: {
          query: {
            type: 'string',
            description:
              'Optional free-text filter matched against the entry title and menu path.'
          },
          kind: {
            type: 'string',
            enum: ['window', 'report', 'process', 'processDefinition', 'form', 'view'],
            description: 'Optional filter by entry type. Omit to include every type.'
          },
          limit: {
            type: 'number',
            minimum: 1,
            maximum: 200,
            description: 'Maximum number of entries to return. Defaults to 50.'
          }
        }
      },
      execute: async function(input) {
        var args = input || {},
          entries = OB.WebMCP.findMenuEntries(args.query, args.kind, args.limit);
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify({ count: entries.length, entries: entries })
            }
          ]
        };
      }
    },
    {
      name: 'etendo_open_window',
      description:
        'Open an Etendo window in a new tab of the current session and focus it. ' +
        'Identify the target by windowId (preferred), tabId, or exact title as returned ' +
        'by etendo_list_windows. Opens the window in grid mode; it does not modify data.',
      inputSchema: {
        type: 'object',
        properties: {
          windowId: { type: 'string', description: 'AD_Window_ID of the window to open.' },
          tabId: { type: 'string', description: 'AD_Tab_ID of the tab to open.' },
          title: {
            type: 'string',
            description: 'Window title, used only when no id is available.'
          },
          recordId: {
            type: 'string',
            description:
              'Optional record id to open directly in form view instead of the grid.'
          }
        }
      },
      execute: async function(input) {
        var args = input || {},
          candidates = OB.WebMCP.resolveEntry(args);

        if (candidates.length === 0) {
          return {
            isError: true,
            content: [
              {
                type: 'text',
                text:
                  'No menu entry found. Call etendo_list_windows first to get a valid windowId.'
              }
            ]
          };
        }
        if (candidates.length > 1) {
          return {
            isError: true,
            content: [
              {
                type: 'text',
                text:
                  'Ambiguous reference, several entries match: ' +
                  JSON.stringify(candidates)
              }
            ]
          };
        }

        var entry = candidates[0];

        if (entry.kind !== 'window') {
          // Non-window entries (reports, processes, views) go through the menu path.
          OB.Layout.ViewManager.openView(entry.viewId, {
            viewId: entry.viewId,
            tabTitle: entry.title
          });
        } else {
          OB.Utilities.openView(
            entry.windowId,
            entry.tabId,
            entry.title,
            args.recordId || null
          );
        }

        return {
          content: [
            {
              type: 'text',
              text: 'Opened "' + entry.path + '" (windowId ' + entry.windowId + ').'
            }
          ]
        };
      }
    },
    {
      name: 'etendo_list_open_tabs',
      description:
        'List the tabs currently open in the Etendo workspace, including which one is ' +
        'active. Use it to know where the user is before navigating.',
      inputSchema: { type: 'object', properties: {} },
      execute: async function() {
        return {
          content: [
            { type: 'text', text: JSON.stringify(OB.WebMCP.listOpenTabs()) }
          ]
        };
      }
    },
    {
      name: 'etendo_focus_tab',
      description:
        'Bring an already open Etendo tab to the front. Use the viewTabId returned by ' +
        'etendo_list_open_tabs.',
      inputSchema: {
        type: 'object',
        properties: {
          viewTabId: { type: 'string', description: 'viewTabId of the tab to focus.' }
        },
        required: ['viewTabId']
      },
      execute: async function(input) {
        var args = input || {},
          known = OB.WebMCP.listOpenTabs().some(function(t) {
            return t.viewTabId === args.viewTabId;
          });
        if (!known) {
          return {
            isError: true,
            content: [{ type: 'text', text: 'Unknown viewTabId: ' + args.viewTabId }]
          };
        }
        OB.MainView.TabSet.selectTab(args.viewTabId);
        return {
          content: [{ type: 'text', text: 'Focused tab ' + args.viewTabId + '.' }]
        };
      }
    },
    {
      name: 'etendo_clear_filters',
      description:
        'Clear the column filters of the grid in an Etendo window, so the grid shows all the ' +
        'records the user can see. Use it when a grid shows no results because of a leftover ' +
        'filter, or when the user asks to reset or clear the filters. Operates on the active ' +
        'tab unless a viewTabId is given.',
      inputSchema: {
        type: 'object',
        properties: {
          viewTabId: {
            type: 'string',
            description:
              'Tab to act on, from etendo_list_open_tabs. Defaults to the active tab.'
          },
          includeImplicit: {
            type: 'boolean',
            description:
              'Also drop the implicit filter the window was opened with (for example the ' +
              'transactional filter that limits a grid to recent draft documents). Defaults ' +
              'to false, which clears only the filters the user can see in the filter row.'
          }
        }
      },
      execute: async function(input) {
        var args = input || {},
          view = OB.WebMCP.getActiveView(args.viewTabId),
          grid = view && view.viewGrid,
          clearedImplicit = false;

        if (!grid || !grid.clearFilter) {
          return {
            isError: true,
            content: [
              {
                type: 'text',
                text:
                  'That tab has no filterable grid. Only standard windows do; the Workspace, ' +
                  'processes and external pages do not.'
              }
            ]
          };
        }

        if (args.includeImplicit) {
          // The implicit clause lives on the grid, and the window flag keeps it
          // from being restored when the grid rebuilds its state.
          grid.filterClause = null;
          if (view.standardWindow) {
            view.standardWindow.emptyFilterClause = true;
          }
          clearedImplicit = true;
        }
        // No arguments: this is what the grid's own "Clear Filter(s)" link calls.
        grid.clearFilter();

        return {
          content: [
            {
              type: 'text',
              text:
                'Cleared the filters of "' +
                (view.tabTitle || view.entity || 'the grid') +
                '"' +
                (clearedImplicit ? ', including the implicit filter' : '') +
                '. The grid is reloading.'
            }
          ]
        };
      }
    },
    {
      name: 'etendo_new_record',
      description:
        'Open the empty form to create a new record in an Etendo window. Use it when the user ' +
        'wants to add or create a record. It only opens the blank form and fills nothing in; ' +
        'the user still types the values and saves. Operates on the active tab unless a ' +
        'viewTabId is given.',
      inputSchema: {
        type: 'object',
        properties: {
          viewTabId: {
            type: 'string',
            description:
              'Tab to act on, from etendo_list_open_tabs. Defaults to the active tab.'
          }
        }
      },
      execute: async function(input) {
        var args = input || {},
          view = OB.WebMCP.getActiveView(args.viewTabId);

        if (!view || !view.newDocument) {
          return {
            isError: true,
            content: [
              {
                type: 'text',
                text:
                  'That tab cannot create records. Open a standard window first with ' +
                  'etendo_open_window.'
              }
            ]
          };
        }
        if (view.readOnly || view.isReadOnly) {
          return {
            isError: true,
            content: [
              { type: 'text', text: 'That window is read-only for the current role.' }
            ]
          };
        }

        // Routes through doActionAfterAutoSave, so a half-edited record is saved
        // or the user is prompted before the blank form replaces it.
        view.newDocument();

        return {
          content: [
            {
              type: 'text',
              text:
                'Opened the new-record form in "' +
                (view.tabTitle || view.entity || 'the window') +
                '". It is empty and unsaved; the user fills it in and saves.'
            }
          ]
        };
      }
    },
    {
      name: 'etendo_list_fields',
      description:
        'List the fields of the record form currently open in an Etendo window, with their ' +
        'current values, labels, types and whether they are required or read-only. Call this ' +
        'BEFORE etendo_set_fields to learn the exact field names to write to. Fields marked ' +
        'isReference expect a record id as value, not the text shown on screen.',
      inputSchema: {
        type: 'object',
        properties: {
          viewTabId: {
            type: 'string',
            description: 'Tab to read. Defaults to the active tab.'
          },
          onlyWritable: {
            type: 'boolean',
            description:
              'Return only the fields that can be written to. Useful to keep the list short.'
          },
          includeNonDataFields: {
            type: 'boolean',
            description:
              'Also include the form section headers and canvases (Audit, Notes, ' +
              'Attachments). They are not writable; off by default.'
          }
        }
      },
      execute: async function(input) {
        var args = input || {},
          form = getEditForm(args.viewTabId),
          fields;
        if (!form) {
          return {
            isError: true,
            content: [
              {
                type: 'text',
                text:
                  'No record form is open in that tab. Use etendo_new_record to create one, ' +
                  'or open an existing record first.'
              }
            ]
          };
        }
        fields = (form.getFields() || []).map(describeField).filter(function(f) {
          if (!f.name) {
            return false;
          }
          // Section headers and canvases are not writable fields; hide them
          // unless explicitly asked for, so the agent does not try to fill them.
          if (!f.isDataField && !args.includeNonDataFields) {
            return false;
          }
          return !args.onlyWritable || !f.readOnly;
        });
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify({ count: fields.length, fields: fields })
            }
          ]
        };
      }
    },
    {
      name: 'etendo_set_fields',
      description:
        'Write values into the record form open in an Etendo window. Takes several fields at ' +
        'once and runs the callouts, so fields that depend on the ones written are ' +
        'recalculated. Use the exact field names from etendo_list_fields, and pass record ids ' +
        'for fields marked isReference. This only fills the form in; nothing is stored until ' +
        'etendo_save_record runs.',
      inputSchema: {
        type: 'object',
        properties: {
          fields: {
            type: 'object',
            description:
              'Field name to value, e.g. {"documentNo": "SO-1", "description": "text"}. ' +
              'For reference fields you may pass either the record id or the text the user ' +
              'would read, such as a business partner name; it is looked up automatically ' +
              'and the tool reports back if it is ambiguous. Use null to clear a field.'
          },
          viewTabId: {
            type: 'string',
            description: 'Tab to write to. Defaults to the active tab.'
          }
        },
        required: ['fields']
      },
      execute: async function(input) {
        var args = input || {},
          view = OB.WebMCP.getActiveView(args.viewTabId),
          form = getEditForm(args.viewTabId),
          names,
          written = [],
          rejected = [],
          unresolved = [],
          resolvedRefs = [],
          lookup,
          exact,
          chosen,
          value,
          i,
          name,
          item;

        if (!form) {
          return {
            isError: true,
            content: [
              { type: 'text', text: 'No record form is open in that tab.' }
            ]
          };
        }
        names = Object.keys(args.fields || {});
        if (names.length === 0) {
          return {
            isError: true,
            content: [{ type: 'text', text: 'No fields given.' }]
          };
        }

        // Check every name up front: a partially written form is worse than one
        // that was not touched, because the user cannot tell what got through.
        for (i = 0; i < names.length; i++) {
          item = form.getField(names[i]);
          if (!item) {
            rejected.push({ field: names[i], reason: 'no such field' });
          } else if (!item.inpColumnName) {
            // A section header or canvas: setting it silently writes nowhere.
            rejected.push({
              field: names[i],
              reason: 'not a data field (form section or canvas)'
            });
          } else if (item.disabled || item.readOnly) {
            rejected.push({ field: names[i], reason: 'read-only' });
          } else if (
            item.getClassName &&
            item.getClassName() === 'OBSearchItem'
          ) {
            // Its value is chosen in a search popup, which only the user can
            // complete; writing it directly would leave the field inconsistent.
            rejected.push({
              field: names[i],
              reason:
                'search field: it opens a popup the user has to complete, ' +
                'so it cannot be filled in from here'
            });
          }
        }
        if (rejected.length > 0) {
          return {
            isError: true,
            content: [
              {
                type: 'text',
                text:
                  'Nothing was written. Call etendo_list_fields for the valid names. ' +
                  'Rejected: ' + JSON.stringify(rejected)
              }
            ]
          };
        }

        for (i = 0; i < names.length; i++) {
          name = names[i];
          item = form.getField(name);
          value = args.fields[name];

          if (usesDropdown(item) && value !== null && value !== undefined) {
            lookup = await pickFromDropdown(item, value);
            exact = lookup.matches.filter(function(m) {
              return (
                String(m.value) === String(value) ||
                String(m.map).toLowerCase() === String(value).toLowerCase()
              );
            });
            chosen = exact.length === 1 ? exact[0] : null;
            if (!chosen && lookup.matches.length === 1) {
              chosen = lookup.matches[0];
            }
            if (!chosen) {
              unresolved.push({
                field: name,
                value: value,
                reason:
                  lookup.matches.length === 0
                    ? lookup.timedOut
                      ? 'the drop-down did not load in time'
                      : 'no option matches this value'
                    : 'several options match; pick one exactly',
                candidates: lookup.matches.slice(0, 8).map(function(m) {
                  return m.map;
                })
              });
              continue;
            }
            // Clicking the row is what commits the value AND its label, and it
            // is what fires the callout.
            chooseOption(item, lookup.list, chosen);
            await waitForCallouts(form);
            resolvedRefs.push({ field: name, resolvedTo: chosen.map });
            written.push(name);
            continue;
          }

          form.setItemValue(item, value);
          // handleItemChange is the path the UI itself uses: it fires the
          // onchange handlers and the callout when the field has one.
          item._hasChanged = true;
          form.handleItemChange(item);
          await waitForCallouts(form);
          written.push(name);
        }

        if (unresolved.length > 0) {
          return {
            isError: true,
            content: [
              {
                type: 'text',
                text: JSON.stringify({
                  written: written,
                  unresolved: unresolved,
                  message:
                    'Some reference fields could not be resolved. Pick one of the ' +
                    'candidates and call etendo_set_fields again for those fields.'
                })
              }
            ]
          };
        }

        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify({
                tab: view ? view.tabTitle : null,
                written: written,
                resolvedReferences: resolvedRefs,
                note: 'Filled in but not saved. Call etendo_save_record to store it.',
                valuesAfterCallouts: (form.getFields() || [])
                  .map(describeField)
                  .filter(function(f) {
                    return written.indexOf(f.name) !== -1;
                  })
              })
            }
          ]
        };
      }
    },
    {
      name: 'etendo_save_record',
      description:
        'Save the record currently being edited in an Etendo window. This writes to the ' +
        'database. The user is asked to confirm before anything is stored.',
      inputSchema: {
        type: 'object',
        properties: {
          viewTabId: {
            type: 'string',
            description: 'Tab to save. Defaults to the active tab.'
          }
        }
      },
      execute: async function(input) {
        var args = input || {},
          view = OB.WebMCP.getActiveView(args.viewTabId),
          form = getEditForm(args.viewTabId),
          ok;

        if (!view || !form || !view.saveRow) {
          return {
            isError: true,
            content: [
              { type: 'text', text: 'No record is being edited in that tab.' }
            ]
          };
        }
        await waitForCallouts(form);

        ok = await askUser(
          'Claude wants to save this record in "' +
            (view.tabTitle || 'this window') +
            '". Save it?'
        );
        if (!ok) {
          return declinedResult('the save');
        }

        view.saveRow();

        return {
          content: [
            {
              type: 'text',
              text:
                'Save submitted for "' +
                (view.tabTitle || 'the window') +
                '". Validation errors, if any, appear in the window message bar; ' +
                'call etendo_list_fields to read the resulting state.'
            }
          ]
        };
      }
    },
    {
      name: 'etendo_list_buttons',
      description:
        'List the buttons available in an Etendo window: the toolbar actions (save, new, ' +
        'delete, refresh, export...) and the process buttons the window adds. Shows which ones ' +
        'are currently disabled. Call this before etendo_click_button.',
      inputSchema: {
        type: 'object',
        properties: {
          viewTabId: {
            type: 'string',
            description: 'Tab to inspect. Defaults to the active tab.'
          }
        }
      },
      execute: async function(input) {
        var args = input || {},
          view = OB.WebMCP.getActiveView(args.viewTabId),
          buttons;
        if (!view) {
          return {
            isError: true,
            content: [{ type: 'text', text: 'That tab has no toolbar.' }]
          };
        }
        buttons = collectButtons(view).map(function(b) {
          return { id: b.id, label: b.label, kind: b.kind, disabled: b.disabled };
        });
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify({ count: buttons.length, buttons: buttons })
            }
          ]
        };
      }
    },
    {
      name: 'etendo_click_button',
      description:
        'Press a button in an Etendo window, identified by the id returned by ' +
        'etendo_list_buttons. Buttons that change or delete data, and process buttons, ask the ' +
        'user to confirm first.',
      inputSchema: {
        type: 'object',
        properties: {
          buttonId: {
            type: 'string',
            description: 'Button id from etendo_list_buttons, e.g. "refresh" or "eliminate".'
          },
          viewTabId: {
            type: 'string',
            description: 'Tab to act on. Defaults to the active tab.'
          }
        },
        required: ['buttonId']
      },
      execute: async function(input) {
        var args = input || {},
          view = OB.WebMCP.getActiveView(args.viewTabId),
          buttons = collectButtons(view),
          match = null,
          i,
          needsConfirmation,
          ok;

        for (i = 0; i < buttons.length; i++) {
          if (
            buttons[i].id === args.buttonId ||
            (buttons[i].label || '').toLowerCase() === String(args.buttonId).toLowerCase()
          ) {
            match = buttons[i];
            break;
          }
        }
        if (!match) {
          return {
            isError: true,
            content: [
              {
                type: 'text',
                text:
                  'No such button. Available: ' +
                  buttons
                    .map(function(b) {
                      return b.id;
                    })
                    .join(', ')
              }
            ]
          };
        }
        if (match.disabled) {
          return {
            isError: true,
            content: [
              {
                type: 'text',
                text:
                  'The button "' + match.id + '" is disabled right now, so pressing it would ' +
                  'do nothing. It usually needs a selected record or pending changes.'
              }
            ]
          };
        }

        // Read-only actions go straight through; anything that writes, deletes
        // or runs a process asks first.
        needsConfirmation =
          match.kind === 'process' ||
          ['save', 'saveclose', 'savecloseX', 'eliminate', 'clone'].indexOf(match.id) !== -1;

        if (needsConfirmation) {
          ok = await askUser(
            'Claude wants to press "' +
              match.label +
              '" in "' +
              (view.tabTitle || 'this window') +
              '". This changes data. Continue?'
          );
          if (!ok) {
            return declinedResult('pressing "' + match.label + '"');
          }
        }

        pointAt(
          match.button.getPageRect
            ? {
                x: match.button.getPageLeft() + 12,
                y: match.button.getPageTop() + 12
              }
            : null,
          'Pressing ' + match.label
        );
        pulsePointer();
        match.button.action();

        return {
          content: [
            {
              type: 'text',
              text:
                'Pressed "' + match.label + '". Processes may open a dialog that only the ' +
                'user can complete.'
            }
          ]
        };
      }
    },
    {
      name: 'etendo_read_screen',
      description:
        'Read what the user currently has on screen in Etendo: which window is open, whether ' +
        'it is showing the record list or a single record form, any message shown to the user ' +
        '(including the validation errors that appear after a failed save), the records ' +
        'visible in the list, and the sub-tabs available. Call this to orient yourself before ' +
        'acting, and after saving to find out whether it worked.',
      inputSchema: {
        type: 'object',
        properties: {
          viewTabId: {
            type: 'string',
            description: 'Tab to read. Defaults to the active tab.'
          },
          maxRecords: {
            type: 'number',
            minimum: 1,
            maximum: 100,
            description: 'How many list rows to return. Defaults to 20.'
          }
        }
      },
      execute: async function(input) {
        var args = input || {},
          view = OB.WebMCP.getActiveView(args.viewTabId),
          grid,
          bar,
          columns,
          limit = args.maxRecords || 20,
          rows = [],
          total,
          i,
          screen;

        if (!view) {
          return {
            isError: true,
            content: [
              {
                type: 'text',
                text:
                  'That tab is not a standard window. Use etendo_list_open_tabs to see what ' +
                  'is open.'
              }
            ]
          };
        }

        screen = {
          window: view.tabTitle || null,
          entity: view.entity || null,
          mode: view.isShowingForm ? 'form' : 'list',
          subTabs:
            view.childTabSet && view.childTabSet.tabs
              ? view.childTabSet.tabs.map(function(t) {
                  return t.title;
                })
              : []
        };

        // The message bar is where Etendo reports validation errors, so it is
        // the first thing worth reporting back.
        bar = view.messageBar;
        if (bar && bar.isVisible && bar.isVisible() && bar.text) {
          screen.message = {
            type: bar.type || null,
            text: String(bar.text).replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim()
          };
        } else {
          screen.message = null;
        }

        if (view.isShowingForm && view.viewForm) {
          screen.formFields = (view.viewForm.getFields() || [])
            .map(describeField)
            .filter(function(f) {
              return f.isDataField;
            });
        }

        grid = view.viewGrid;
        if (grid && grid.data) {
          columns = (grid.getFields() || [])
            .map(function(f) {
              return f.name;
            })
            .filter(isDataColumn);
          total = grid.data.getLength ? grid.data.getLength() : 0;
          for (i = 0; i < Math.min(total, limit); i++) {
            if (grid.data.get(i)) {
              rows.push(summarizeRecord(grid.data.get(i), columns));
            }
          }
          screen.list = {
            columns: columns,
            totalRows: total,
            returnedRows: rows.length,
            rows: rows
          };
          screen.selected = (grid.getSelectedRecords() || []).map(function(r) {
            return { id: r.id, identifier: r._identifier };
          });
        }

        return {
          content: [{ type: 'text', text: JSON.stringify(screen) }]
        };
      }
    },
    {
      name: 'etendo_find_records',
      description:
        'Search the records of the window currently open, by filtering on its columns. The ' +
        'search runs against the database, not only the rows already loaded, and it leaves the ' +
        'grid filtered on screen. Use it to locate the record the user is talking about and ' +
        'get its id, which etendo_edit_record needs.',
      inputSchema: {
        type: 'object',
        properties: {
          criteria: {
            type: 'object',
            description:
              'Column name to value, matched as "contains" for text, e.g. ' +
              '{"name": "Standard"}. Use the column names from etendo_read_screen. ' +
              'Omit to clear the filter and list everything.'
          },
          viewTabId: {
            type: 'string',
            description: 'Tab to search. Defaults to the active tab.'
          },
          maxRecords: {
            type: 'number',
            minimum: 1,
            maximum: 100,
            description: 'How many rows to return. Defaults to 20.'
          }
        }
      },
      execute: async function(input) {
        var args = input || {},
          view = OB.WebMCP.getActiveView(args.viewTabId),
          grid = view && view.viewGrid,
          criteria = args.criteria || {},
          names = Object.keys(criteria),
          columns,
          limit = args.maxRecords || 20,
          settled,
          total,
          rows = [],
          i;

        if (!grid || !grid.filterData) {
          return {
            isError: true,
            content: [
              { type: 'text', text: 'That tab has no record list to search.' }
            ]
          };
        }

        // filterData is what the grid's own filter row calls. It does not invoke
        // a callback for a ResultSet-backed grid, so the load is awaited by
        // watching the ResultSet settle instead.
        if (names.length) {
          grid.filterData({
            _constructor: 'AdvancedCriteria',
            operator: 'and',
            criteria: names.map(function(n) {
              return {
                fieldName: n,
                operator: typeof criteria[n] === 'string' ? 'iContains' : 'equals',
                value: criteria[n]
              };
            })
          });
        } else {
          grid.clearFilter();
        }

        settled = await waitForGridLoad(grid, 20000);

        columns = (grid.getFields() || [])
          .map(function(f) {
            return f.name;
          })
          .filter(isDataColumn);

        total = grid.data && grid.data.getLength ? grid.data.getLength() : 0;
        for (i = 0; i < Math.min(total, limit); i++) {
          if (grid.data.get(i)) {
            rows.push(summarizeRecord(grid.data.get(i), columns));
          }
        }

        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify({
                matched: total,
                returned: rows.length,
                complete: settled,
                records: rows
              })
            }
          ]
        };
      }
    },
    {
      name: 'etendo_edit_record',
      description:
        'Open an existing record in the form so its fields can be changed. Identify it by the ' +
        'id returned by etendo_find_records or etendo_read_screen. After this, use ' +
        'etendo_set_fields to change values and etendo_save_record to store them.',
      inputSchema: {
        type: 'object',
        properties: {
          recordId: {
            type: 'string',
            description: 'Id of the record to open, from etendo_find_records.'
          },
          viewTabId: {
            type: 'string',
            description: 'Tab to act on. Defaults to the active tab.'
          }
        },
        required: ['recordId']
      },
      execute: async function(input) {
        var args = input || {},
          view = OB.WebMCP.getActiveView(args.viewTabId),
          grid = view && view.viewGrid,
          record = null,
          i,
          total;

        if (!grid || !view.editRecord) {
          return {
            isError: true,
            content: [{ type: 'text', text: 'That tab cannot edit records.' }]
          };
        }

        total = grid.data && grid.data.getLength ? grid.data.getLength() : 0;
        for (i = 0; i < total; i++) {
          if (grid.data.get(i) && grid.data.get(i).id === args.recordId) {
            record = grid.data.get(i);
            break;
          }
        }
        if (!record) {
          return {
            isError: true,
            content: [
              {
                type: 'text',
                text:
                  'That record is not among the rows currently loaded. Run ' +
                  'etendo_find_records first so it is fetched, then retry.'
              }
            ]
          };
        }

        grid.selectSingleRecord(record);
        view.editRecord(record);

        return {
          content: [
            {
              type: 'text',
              text:
                'Opened "' +
                (record._identifier || args.recordId) +
                '" for editing. Change values with etendo_set_fields, then call ' +
                'etendo_save_record.'
            }
          ]
        };
      }
    },
    {
      name: 'etendo_get_filters',
      description:
        'Read the filters currently applied to the grid of an Etendo window: which column, ' +
        'which comparison and which value, exactly as the user sees them in the filter row. ' +
        'Also reports the implicit filter the window may have been opened with, which is not ' +
        'shown in the filter row but does restrict what the grid returns.',
      inputSchema: {
        type: 'object',
        properties: {
          viewTabId: {
            type: 'string',
            description: 'Tab to read. Defaults to the active tab.'
          }
        }
      },
      execute: async function(input) {
        var args = input || {},
          view = OB.WebMCP.getActiveView(args.viewTabId),
          grid = view && view.viewGrid,
          filters;

        if (!grid || !grid.getCriteria) {
          return {
            isError: true,
            content: [
              { type: 'text', text: 'That tab has no filterable grid.' }
            ]
          };
        }
        filters = readFilters(grid);
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify({
                window: view.tabTitle || null,
                filterableColumns: (grid.getFields() || [])
                  .map(function(f) {
                    return f.name;
                  })
                  .filter(isDataColumn),
                activeFilters: filters,
                filterCount: filters.length,
                // Not visible in the filter row, but it still limits the rows.
                implicitFilter: grid.filterClause || null,
                rowsShowing:
                  grid.data && grid.data.getLength ? grid.data.getLength() : null
              })
            }
          ]
        };
      }
    },
    {
      name: 'etendo_set_filters',
      description:
        'Filter the grid of an Etendo window by one or more columns. The filters are written ' +
        'into the filter row, so the user sees exactly what was applied and can change it by ' +
        'hand afterwards. Use the column names from etendo_get_filters. This only narrows what ' +
        'is displayed; it changes no data.',
      inputSchema: {
        type: 'object',
        properties: {
          filters: {
            type: 'array',
            description:
              'The filters to apply. Text columns default to "contains", everything else to ' +
              '"equals".',
            items: {
              type: 'object',
              properties: {
                column: { type: 'string', description: 'Column name to filter on.' },
                operator: {
                  type: 'string',
                  enum: [
                    'iContains',
                    'iStartsWith',
                    'equals',
                    'notEqual',
                    'greaterThan',
                    'lessThan',
                    'greaterOrEqual',
                    'lessOrEqual',
                    'isNull',
                    'notNull'
                  ],
                  description: 'How to compare. Optional.'
                },
                value: {
                  description: 'Value to compare against. Omit for isNull/notNull.'
                }
              },
              required: ['column']
            }
          },
          mode: {
            type: 'string',
            enum: ['replace', 'add'],
            description:
              'replace (default) discards the filters already applied; add keeps them and ' +
              'narrows further.'
          },
          viewTabId: {
            type: 'string',
            description: 'Tab to filter. Defaults to the active tab.'
          }
        },
        required: ['filters']
      },
      execute: async function(input) {
        var args = input || {},
          view = OB.WebMCP.getActiveView(args.viewTabId),
          grid = view && view.viewGrid,
          incoming = args.filters || [],
          valid,
          rejected = [],
          resolved = [],
          criteria,
          settled,
          i,
          f,
          field;

        if (!grid || !grid.filterEditor) {
          return {
            isError: true,
            content: [
              { type: 'text', text: 'That tab has no filterable grid.' }
            ]
          };
        }
        if (incoming.length === 0) {
          return {
            isError: true,
            content: [
              {
                type: 'text',
                text: 'No filters given. Use etendo_clear_filters to remove them instead.'
              }
            ]
          };
        }

        valid = (grid.getFields() || [])
          .map(function(x) {
            return x.name;
          })
          .filter(isDataColumn);

        // Validate everything first: a half-applied filter set shows the user a
        // grid that matches neither what they asked for nor what it says.
        for (i = 0; i < incoming.length; i++) {
          f = incoming[i];
          if (valid.indexOf(f.column) === -1) {
            rejected.push({ column: f.column, reason: 'not a filterable column' });
          } else if (f.operator && FILTER_OPERATORS.indexOf(f.operator) === -1) {
            rejected.push({ column: f.column, reason: 'unknown operator ' + f.operator });
          }
        }
        if (rejected.length > 0) {
          return {
            isError: true,
            content: [
              {
                type: 'text',
                text:
                  'Nothing was applied. Filterable columns are: ' + valid.join(', ') +
                  '. Rejected: ' + JSON.stringify(rejected)
              }
            ]
          };
        }

        for (i = 0; i < incoming.length; i++) {
          f = incoming[i];
          field = grid.getField(f.column);
          resolved.push({
            column: f.column,
            operator:
              f.operator ||
              (typeof f.value === 'string' ? 'iContains' : 'equals'),
            value: f.value === undefined ? null : f.value
          });
        }

        if (args.mode === 'add') {
          // Keep the filters already on screen, minus the columns being reset.
          resolved = readFilters(grid)
            .filter(function(existing) {
              return !resolved.some(function(r) {
                return r.column === existing.column;
              });
            })
            .concat(resolved);
        }

        criteria = buildCriteria(resolved);
        // Writing the filter row first is what makes the result legible: the
        // user sees the same filter the agent applied, and can edit it.
        grid.filterEditor.setValuesAsCriteria(criteria);
        grid.filterData(criteria);
        settled = await waitForGridLoad(grid, 20000);

        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify({
                applied: resolved,
                mode: args.mode || 'replace',
                rowsShowing:
                  grid.data && grid.data.getLength ? grid.data.getLength() : null,
                complete: settled
              })
            }
          ]
        };
      }
    }
  ];



  // A refusal is not a failure. The agent side needs to tell them apart, and it
  // cannot do that from the wording -- this dialog is the one string the end
  // user reads, so it will be translated. The flag carries the distinction.
  function declinedResult(what) {
    return {
      isError: true,
      declined: true,
      content: [
        {
          type: 'text',
          text: JSON.stringify({
            declined: true,
            message:
              'The user declined this action, so ' + what + ' was not performed. ' +
              'Do not retry it; ask what they would like to do instead.'
          })
        }
      ]
    };
  }


  // Waits for a grid's ResultSet to finish loading after a filter change.
  function waitForGridLoad(grid, timeoutMs) {
    var deadline = Date.now() + (timeoutMs || 20000);
    return new Promise(function(resolve) {
      (function poll() {
        var data = grid.data;
        if (
          data &&
          (!data.lengthIsKnown || data.lengthIsKnown()) &&
          data.loading !== true
        ) {
          // Let the row batch settle before reading it.
          setTimeout(function() {
            resolve(true);
          }, 400);
          return;
        }
        if (Date.now() > deadline) {
          resolve(false);
          return;
        }
        setTimeout(poll, 200);
      })();
    });
  }

  // Grid columns that carry no data: the selection checkbox and the edit pencil.
  function isDataColumn(name) {
    return name && name.charAt(0) !== '_';
  }

  // Trims a grid record down to what is worth sending to a model: the id, the
  // human-readable identifier, and the columns actually shown in the grid.
  function summarizeRecord(record, columns) {
    var out = { id: record.id, identifier: record._identifier };
    columns.forEach(function(c) {
      if (record[c] !== undefined) {
        out[c] = record[c + '$_identifier'] || record[c];
      }
    });
    return out;
  }


  var FILTER_OPERATORS = [
    'iContains',
    'iStartsWith',
    'equals',
    'notEqual',
    'greaterThan',
    'lessThan',
    'greaterOrEqual',
    'lessOrEqual',
    'isNull',
    'notNull'
  ];

  // Flattens the grid criteria into one row per filtered column, dropping the
  // pseudo-columns the filter editor keeps for the checkbox and the edit link.
  function readFilters(grid) {
    var criteria = grid.getCriteria ? grid.getCriteria() : null,
      list = (criteria && criteria.criteria) || [];
    return list
      .filter(function(c) {
        return c.fieldName && isDataColumn(c.fieldName);
      })
      .map(function(c) {
        return {
          column: c.fieldName,
          operator: c.operator || 'equals',
          value: c.value === undefined ? null : c.value
        };
      });
  }

  function buildCriteria(filters) {
    return {
      _constructor: 'AdvancedCriteria',
      operator: 'and',
      criteria: filters.map(function(f) {
        var entry = { fieldName: f.column, operator: f.operator };
        if (f.operator !== 'isNull' && f.operator !== 'notNull') {
          entry.value = f.value;
        }
        return entry;
      })
    };
  }


  // --- Reference fields -----------------------------------------------------
  //
  // Reference fields (OBSelectorItem and friends) store an id but display a
  // label, and they keep the two paired in a valueMap. Writing the raw id with
  // setItemValue leaves the item with no label to show, and the item then drops
  // the value again -- the field ends up empty even though the callout already
  // fired. setValueFromRecord({value, map}) is the supported way in: it stores
  // the id, records the label, and keeps the grid in sync.

  function isReferenceItem(item) {
    return typeof item.setValueFromRecord === 'function';
  }


  // --- Visible pointer --------------------------------------------------------
  //
  // The tools drive the UI directly, so without this the screen changes with no
  // visible cause. A floating Copilot marker follows whatever the agent is
  // touching and pulses when it clicks, which makes the run watchable and makes
  // a wrong target obvious.

  OB.WebMCP.showPointer = true;

  var pointerNode = null,
    pointerLabelNode = null,
    haloNode = null,
    pointerHideTimer = null;

  function buildPointer() {
    var style;
    if (pointerNode) {
      return pointerNode;
    }
    if (!document.getElementById('OBWebMCPPointerStyle')) {
      style = document.createElement('style');
      style.id = 'OBWebMCPPointerStyle';
      style.textContent =
        '#OBWebMCPPointer,#OBWebMCPHalo{position:fixed;z-index:2147483000;' +
        'pointer-events:none;left:0;top:0;opacity:0;' +
        'transition:transform .3s cubic-bezier(.22,.61,.36,1),opacity .2s,' +
        'width .3s,height .3s}' +
        '#OBWebMCPPointer{width:26px;height:26px;border-radius:50%;' +
        'background:#202452 center/17px 17px no-repeat;' +
        'box-shadow:0 2px 8px rgba(0,0,0,.35);border:2px solid #fff}' +
        '#OBWebMCPHalo{border:2px solid #202452;border-radius:4px;' +
        'box-shadow:0 0 0 3px rgba(32,36,82,.18)}' +
        '#OBWebMCPLabel{position:fixed;z-index:2147483001;pointer-events:none;' +
        'left:0;top:0;opacity:0;background:#202452;color:#fff;' +
        'font:11px/1.4 sans-serif;padding:3px 9px;border-radius:9px;' +
        'white-space:nowrap;max-width:300px;overflow:hidden;' +
        'text-overflow:ellipsis;box-shadow:0 2px 8px rgba(0,0,0,.3);' +
        'transition:transform .3s cubic-bezier(.22,.61,.36,1),opacity .2s}' +
        '#OBWebMCPPointer.obmcp-click{animation:obmcpPulse .45s ease-out}' +
        '@keyframes obmcpPulse{0%{box-shadow:0 0 0 0 rgba(32,36,82,.55)}' +
        '70%{box-shadow:0 0 0 13px rgba(32,36,82,0)}' +
        '100%{box-shadow:0 2px 8px rgba(0,0,0,.35)}}';
      document.head.appendChild(style);
    }
    haloNode = document.createElement('div');
    haloNode.id = 'OBWebMCPHalo';
    pointerNode = document.createElement('div');
    pointerNode.id = 'OBWebMCPPointer';
    pointerNode.style.backgroundImage =
      'url(' + (OB.Application.contextUrl || '/') + 'web/images/copilot.png)';
    pointerLabelNode = document.createElement('div');
    pointerLabelNode.id = 'OBWebMCPLabel';
    document.body.appendChild(haloNode);
    document.body.appendChild(pointerNode);
    document.body.appendChild(pointerLabelNode);
    return pointerNode;
  }

  // Marks what the agent is touching without covering it: the halo outlines the
  // field, the marker sits just outside it, and the caption goes above. An
  // earlier version placed the marker on top of the field, which hid the very
  // value the user wanted to watch being filled in.
  function pointAt(target, label) {
    var rect, markerX, markerY, labelX, labelY, labelWidth;
    if (!OB.WebMCP.showPointer || typeof document === 'undefined') {
      return;
    }
    try {
      buildPointer();
      if (target && target.getBoundingClientRect) {
        rect = target.getBoundingClientRect();
        if (!rect.width && !rect.height) {
          return;
        }
      } else if (target && typeof target.x === 'number') {
        rect = {
          left: target.x,
          top: target.y,
          width: 0,
          height: 0,
          right: target.x,
          bottom: target.y
        };
      } else {
        return;
      }

      // Outline the target itself.
      haloNode.style.width = rect.width + 'px';
      haloNode.style.height = rect.height + 'px';
      haloNode.style.transform =
        'translate(' + (rect.left - 2) + 'px,' + (rect.top - 2) + 'px)';
      haloNode.style.opacity = rect.width ? '1' : '0';

      // Marker to the left of the field, or to the right when there is no room.
      markerY = rect.top + rect.height / 2 - 13;
      markerX = rect.left - 34;
      if (markerX < 4) {
        markerX = rect.right + 8;
      }
      pointerNode.style.transform =
        'translate(' + markerX + 'px,' + markerY + 'px)';
      pointerNode.style.opacity = '1';

      // Caption above the field, nudged back inside the viewport if needed.
      pointerLabelNode.textContent = label || '';
      labelWidth = pointerLabelNode.offsetWidth || 120;
      labelX = rect.left;
      if (labelX + labelWidth > window.innerWidth - 8) {
        labelX = Math.max(4, window.innerWidth - labelWidth - 8);
      }
      labelY = rect.top - 24;
      if (labelY < 4) {
        labelY = rect.bottom + 6;
      }
      pointerLabelNode.style.transform =
        'translate(' + labelX + 'px,' + labelY + 'px)';
      pointerLabelNode.style.opacity = label ? '1' : '0';

      if (pointerHideTimer) {
        clearTimeout(pointerHideTimer);
      }
      pointerHideTimer = setTimeout(OB.WebMCP.hidePointer, 4000);
    } catch (e) {
      // Never let the decoration break the action it is decorating.
      isc.logWarn('WebMCP pointer: ' + e);
    }
  }

  function pulsePointer() {
    if (!pointerNode || !OB.WebMCP.showPointer) {
      return;
    }
    pointerNode.classList.remove('obmcp-click');
    // Reading offsetWidth restarts the animation.
    void pointerNode.offsetWidth;
    pointerNode.classList.add('obmcp-click');
  }

  OB.WebMCP.hidePointer = function() {
    [pointerNode, haloNode, pointerLabelNode].forEach(function(node) {
      if (node) {
        node.style.opacity = '0';
      }
    });
  };

  // Matches a value against an item's fixed option list, by stored code or by
  // the label the user reads. Returns null when the item has no fixed list.
  function matchStaticOptions(item, text) {
    var map = item.valueMap,
      needle = String(text).toLowerCase(),
      options = [],
      key;
    if (item.optionDataSource || !map) {
      return null;
    }
    for (key in map) {
      if (
        Object.prototype.hasOwnProperty.call(map, key) &&
        map[key] !== null &&
        map[key] !== undefined
      ) {
        options.push({ value: key, map: String(map[key]), staticOption: true });
      }
    }
    if (options.length === 0) {
      return null;
    }
    return options.filter(function(o) {
      return (
        String(o.value).toLowerCase() === needle ||
        o.map.toLowerCase() === needle ||
        o.map.toLowerCase().indexOf(needle) !== -1
      );
    });
  }

  // Fills a reference field the way a person does: click it, type, wait for the
  // drop-down, pick a row.
  //
  // Writing the value through the item API instead does not work. The field
  // stores an id but shows a label, and setting the raw id leaves it with
  // nothing to display, so it drops the value again and the field ends up empty
  // even though the callout already ran. Driving the UI also means every kind
  // of picker behaves correctly without special-casing each one.
  function pickFromDropdown(item, text) {
    var element = item.getDataElement && item.getDataElement(),
      staticOptions;
    if (!element) {
      return Promise.resolve({ matches: [], reason: 'field is not on screen' });
    }

    // A fixed list (OBListItem) already holds every option in its valueMap, so
    // there is nothing to fetch and nothing to type: match against the map and
    // let the item pick the value the way its own drop-down would.
    staticOptions = matchStaticOptions(item, text);
    if (staticOptions) {
      return Promise.resolve({ matches: staticOptions, staticList: true });
    }

    pointAt(element, 'Filling in ' + (item.title || item.name));
    ['mousedown', 'mouseup', 'click'].forEach(function(type) {
      element.dispatchEvent(
        new MouseEvent(type, { bubbles: true, cancelable: true, view: window })
      );
    });
    pulsePointer();
    element.focus();

    return new Promise(function(resolve) {
      var characters = String(text).split(''),
        index = 0;

      // Typed one character at a time: the drop-down filters on key events, and
      // dropping the whole string in at once does not trigger it.
      (function typeNext() {
        var character;
        if (index < characters.length) {
          character = characters[index++];
          element.value = element.value + character;
          element.dispatchEvent(
            new KeyboardEvent('keydown', { bubbles: true, key: character })
          );
          element.dispatchEvent(new Event('input', { bubbles: true }));
          element.dispatchEvent(
            new KeyboardEvent('keyup', { bubbles: true, key: character })
          );
          setTimeout(typeNext, 120);
          return;
        }
        waitForOptions();
      })();

      function waitForOptions() {
        var deadline = Date.now() + 15000;
        setTimeout(function poll() {
          var list = item.pickList,
            data = list && list.data,
            rows = [],
            total,
            i,
            record;

          if (data && (!data.lengthIsKnown || data.lengthIsKnown()) && data.loading !== true) {
            total = data.getLength ? data.getLength() : 0;
            for (i = 0; i < total; i++) {
              record = data.get(i);
              if (record) {
                rows.push({
                  index: i,
                  record: record,
                  value: record[item.valueField || 'id'],
                  map: record[item.displayField || '_identifier']
                });
              }
            }
            resolve({ matches: rows, list: list });
            return;
          }
          if (Date.now() > deadline) {
            resolve({ matches: [], timedOut: true });
            return;
          }
          setTimeout(poll, 250);
        }, 800);
      }
    });
  }

  function chooseOption(item, list, match) {
    var rowElement;
    pointAt(item.getDataElement && item.getDataElement(),
      'Choosing "' + match.map + '"');
    pulsePointer();
    if (match.staticOption) {
      // pickValue is what the fixed-list drop-down calls on click.
      item.pickValue(match.value);
      return;
    }
    rowElement = list.getTableElement && list.getTableElement(match.index);
    if (rowElement) {
      ['mousedown', 'mouseup', 'click'].forEach(function(type) {
        rowElement.dispatchEvent(
          new MouseEvent(type, { bubbles: true, cancelable: true, view: window })
        );
      });
      return;
    }
    list.rowClick(match.record, match.index, 0);
  }

  // True for fields the user fills by picking from a drop-down rather than by
  // typing a literal value.
  //
  // Keyed on filterDataBoundPickList rather than on the item class, because the
  // three picker classes do not share a base: OBSelectorItem and OBFKComboItem
  // fetch their options from a datasource, OBListItem holds a fixed list, and
  // all three answer to this method. Plain items (text, number, date, checkbox,
  // text area) do not, and are written directly.
  //
  // Not covered: OBSearchItem, which opens a search popup instead of a drop-down
  // and has no pick list to drive.
  function usesDropdown(item) {
    return (
      typeof item.filterDataBoundPickList === 'function' &&
      typeof item.getDataElement === 'function'
    );
  }

  // --- Registration --------------------------------------------------------
  //
  // API surface notes (verified against Chrome 152, WebMCP origin trial):
  //  * The namespace is document.modelContext. Chrome 146-149 exposed it as
  //    navigator.modelContext and 150 deprecated that, so both are probed.
  //  * registerTool(descriptor, options) is asynchronous.
  //  * There is no unregisterTool(). Deregistration happens by aborting the
  //    AbortSignal passed in options, so one controller is kept per session.
  //  * Registering a name that is already registered rejects with
  //    InvalidStateError ("Duplicate tool name"), hence the guard below.

  function getModelContext() {
    if (typeof document !== 'undefined' && document.modelContext) {
      return document.modelContext;
    }
    if (navigator.modelContext) {
      return navigator.modelContext;
    }
    return null;
  }

  var abortController = null;

  // Diagnostics: explains why register() did or did not do anything.
  OB.WebMCP.status = function() {
    var mc = getModelContext();
    return {
      apiAvailable: !!mc,
      apiNamespace: !mc
        ? null
        : typeof document !== 'undefined' && document.modelContext
        ? 'document.modelContext'
        : 'navigator.modelContext',
      secureContext: window.isSecureContext,
      menuLoaded: !!(OB.Application && OB.Application.menu),
      registered: !!OB.WebMCP.registered,
      toolNames: TOOLS.map(function(tool) {
        return tool.name;
      }),
      hint: !mc
        ? 'No modelContext: needs Chrome 149+ with WebMCP enabled ' +
          '(chrome://flags/#enable-webmcp-testing) or an origin trial token, ' +
          'over HTTPS or localhost.'
        : !(OB.Application && OB.Application.menu)
        ? 'OB.Application.menu is not loaded yet; call register() after login.'
        : OB.WebMCP.registered
        ? 'Tools already registered.'
        : 'Ready to register.'
    };
  };

  // Returns a promise resolving to the list of tool names actually registered.
  OB.WebMCP.register = function() {
    var mc = getModelContext();
    if (!mc || OB.WebMCP.registered) {
      return Promise.resolve([]);
    }
    // Claim the flag before awaiting so concurrent calls cannot double-register
    // and trip the Duplicate tool name error.
    OB.WebMCP.registered = true;
    abortController = new window.AbortController();

    var options = { signal: abortController.signal };
    return Promise.all(
      TOOLS.map(function(tool) {
        return Promise.resolve(mc.registerTool(tool, options))
          .then(function() {
            return tool.name;
          })
          .catch(function(e) {
            isc.logWarn(
              'WebMCP: could not register tool ' + tool.name + ': ' + e
            );
            return null;
          });
      })
    ).then(function(names) {
      var registered = names.filter(function(n) {
        return n;
      });
      if (registered.length === 0) {
        OB.WebMCP.registered = false;
        abortController = null;
      }
      return registered;
    });
  };

  OB.WebMCP.unregister = function() {
    if (abortController) {
      abortController.abort();
      abortController = null;
    }
    OB.WebMCP.registered = false;
  };

  // Registers once the menu is available. This script may be evaluated either
  // before or after page load depending on resource aggregation order, so try
  // both paths; register() is a no-op when already registered.
  function tryRegister() {
    if (OB.Application && OB.Application.menu) {
      OB.WebMCP.register();
    }
  }

  if (getModelContext()) {
    isc.Page.setEvent('load', tryRegister);
    tryRegister();
    // Drop the tools when the user leaves, so a stale registration cannot
    // outlive the session in a persisted page.
    window.addEventListener('pagehide', function() {
      OB.WebMCP.unregister();
    });
  }
})(OB, isc);
