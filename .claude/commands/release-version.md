Release a new version of the application. The version to release is: `$ARGUMENTS`

If no version was provided in the arguments, stop and ask the user for the version string (format `x.x.x`).

Run the following steps from the `schlierelacht-admin/` directory, in order. Stop and report if any step fails.

1. **Update the version in `pom.xml`** (sets the `<version>` of the project):
   ```bash
   ./mvnw -q versions:set -DnewVersion=$ARGUMENTS -DgenerateBackupPoms=false
   ```

2. **Build the application** to verify the new version compiles and packages cleanly. If this fails, stop and report — do **not** commit, tag, or push a broken build:
   ```bash
   ./mvnw clean package
   ```

3. **Commit the change** (only `pom.xml`, using the established `Release x.x.x` message):
   ```bash
   git add pom.xml && git commit -m "Release $ARGUMENTS"
   ```

4. **Tag the commit** with the bare version (matches existing tags, no `v` prefix):
   ```bash
   git tag $ARGUMENTS
   ```

5. **Push the commit and the tag**:
   ```bash
   git push --follow-tags
   ```

Report the new version, the commit hash, and the tag name when done.
