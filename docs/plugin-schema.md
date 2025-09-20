# Plugin Schemas

Plugins expose credential configuration via PluginProfileDescriptor implementations. A plugin definition that implements PluginProfileProvider can return one or more descriptors describing the profiles it supports. Each descriptor may reference a JSON schema file (via schemaPath) and declare whether additional preparation is required after the user submits the form.

Plugins can also surface other configuration sections that evolve independently (for example, shared settings) by implementing PluginSettingProvider and returning PluginSettingDescriptor instances. Settings descriptors mirror the profile shape—each may contribute its own schema and default values—but do not participate in secret generation hooks.

At startup the PluginSynchronizer collects the descriptors, loads their schemas, and persists a consolidated structure on the plugin record. When @Plugin.schemaPath is provided the referenced JSON schema is still loaded and merged so additional sections remain intact. The persisted shape contains a profiles array describing profile descriptors, a settings array for setting descriptors, plus a profile property that mirrors the first profile descriptor's schema for backward compatibility.

`
GET /plugins/{key}/schema
GET /plugins/{key}/descriptors/{descriptorId}/schema?section=profile|setting
`

The response contains:

- profiles: ordered descriptors with id, label, description, equiresPreparation, defaults, and an embedded schema when provided.
- settings: ordered descriptors with id, label, description, defaults, and embedded schema when provided.
- profile: convenience alias for the first profile descriptor's schema so existing consumers can continue to render a single profile form.

Legacy plugins that still declare @Plugin(schemaPath = ...) are automatically wrapped into a single default profile descriptor, so older bundles continue to function without changes. New plugins should prefer the descriptor APIs when the section has its own lifecycle.

During profile creation, descriptors can override prepareProfile(ProfilePreparationContext) to generate additional secrets (e.g., exchanging an OAuth client for a refresh token). When equiresPreparation() returns 	rue, callers should expect to run that hook before persisting the profile. Settings descriptors are purely declarative and do not expose preparation hooks.
