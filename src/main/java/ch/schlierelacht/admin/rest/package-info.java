/**
 * Public, stateless REST endpoints of the festival API.
 *
 * <h2>Versioning</h2>
 * Every endpoint is mapped under {@code /api/v<n>/<resource>} and is versioned
 * <strong>per resource</strong>: {@code /api/v2/attraction} can exist next to
 * {@code /api/v1/news}. {@link ch.schlierelacht.admin.ArchUnitTest} fails the
 * build if a {@code @RestController} is mapped without a version.
 *
 * <p>The version exists because this backend has a single {@code main} branch
 * while the website runs {@code main} (production) and {@code develop}
 * (staging). Bumping a resource instead of changing it in place lets both
 * branches — and any released build of the mobile app — keep working against
 * the shape they were written for.
 *
 * <h3>When to bump</h3>
 * Bump a resource only on a <em>breaking</em> change to its contract: removing
 * or renaming a field, changing a field's type, making an optional field
 * required, changing a path or a status code, or changing the meaning of an
 * existing value. Additive changes — a new optional field, a new endpoint, a
 * new enum constant handled leniently by clients — stay on the current version.
 *
 * <h3>How to bump</h3>
 * <ol>
 *   <li>Copy the endpoint to a new {@code /api/v<n+1>/<resource>} mapping with
 *       the new shape, keeping the old mapping and its DTO untouched so
 *       existing clients keep their responses.</li>
 *   <li>Point the consumers at the new version one at a time: the website's
 *       {@code develop} branch first, {@code main} when the change ships, the
 *       mobile app when its next build goes out.</li>
 *   <li>Delete the old version once nothing calls it any more.</li>
 * </ol>
 *
 * @see ch.schlierelacht.admin.rest.LegacyApiVersionFilter
 */
package ch.schlierelacht.admin.rest;
