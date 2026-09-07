# Navigation bar components

Canonical original UI navigation widgets and their server-side data/actions.
`UserInfoAccessPolicy` separates optional domain restrictions from the unchanged
active backend role query in `UserInfoComponent`. Its trusted server selection
defaults to `ErpUserInfoAccessPolicy`; platform explicitly selects
`PlatformUserInfoAccessPolicy`. The ERP policy is packaged separately from shared UI.
Existing public component and handler signatures remain compatible.
