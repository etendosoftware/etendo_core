# Javascript Linter Scripts

In order to run these scripts, you need to have npm and nodejs installed.

## Enabling pretxncommit check:
Apply configuration depending on your SCM:

### Git
From repo folder run this command:
```
git config core.hooksPath .githooks
```

### Mercurial
Add these lines in your hgrc file:
```
[hooks]
pretxncommit = <path-to-openbravo>/modules/org.openbravo.client.kernel/jslint/jslint-hg
```

## Running jslint directly

### Core
To run the linter to all js files in the project, run the following in Openbravo root folder:
```
 ./modules/org.openbravo.client.kernel/jslint/jslint
```

### Modules
To run jslint directly for a module, go to the module directory and do:

```
 ../org.openbravo.client.kernel/jslint/jslint
```

### Individual files
You can also use the jslint script to run the linter for a file or a set of files.

```
 ./modules/org.openbravo.client.kernel/jslint/jslint modules/org.openbravo.client.application/web/org.openbravo.client.application/js/utilities/ob-utilities.js
```

Run `jslint -h` to see all options available.

**NOTE:**
 it is possible that you have to set the executable flag on the jslint script in org.openbravo.client.kernel/jslint.

