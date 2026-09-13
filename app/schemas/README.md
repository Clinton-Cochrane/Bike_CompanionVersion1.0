# Room schema fixtures

Room exports production database schemas here through the `room.schemaLocation` KAPT argument.
Commit the generated JSON file for every new `BikeCompanionDatabase` version.

Schema export began at version 12. Earlier versions were not exported, so the repository has no source-verified fixtures for versions 1 through 11. Do not create historical JSON files manually. The existing `ComponentContextMigrationTest` continues to exercise the supported 3→12 migration chain, while `DatabaseSchemaFixtureTest` validates the committed version-12 fixture.
