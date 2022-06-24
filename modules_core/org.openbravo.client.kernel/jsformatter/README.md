# Javascript Formatter Scripts

In order to run these scripts, you need to have npm and nodejs installed.

## Enabling pre-commit checking:
Apply these lines depending on the SCM used:

### Git
From repo folder run this command:
```
git config core.hooksPath .githooks
```

### Mercurial
Add these lines in your hgrc file:
```
[hooks]
pre-commit = <path-to-openbravo>/modules/org.openbravo.client.kernel/jsformatter/jsformatter-hg
```

## Running jslint directly

### Core
To run the formatter to all js files in the project, run the following in Openbravo root folder:
```
 ./modules/org.openbravo.client.kernel/jsformatter/jsformatter
```

### Modules
To run the formatter directly for a module, go to the module directory and do:

```
 ../org.openbravo.client.kernel/jsformatter/jsformatter
```

### Individual files
You can also use the jsformatter script to check format for a file or a set of files.

```
 ./modules/org.openbravo.client.kernel/jsformatter/jsformatter modules/org.openbravo.client.application/web/org.openbravo.client.application/js/utilities/ob-utilities.js
```

Run `jsformatter -h` to see all options available.

**NOTE:**
 it is possible that you have to set the executable flag on the jslint and jscheck scripts in org.openbravo.client.kernel/jslint.
