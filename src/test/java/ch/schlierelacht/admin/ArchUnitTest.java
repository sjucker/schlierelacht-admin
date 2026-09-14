package ch.schlierelacht.admin;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.GeneralCodingRules;
import org.jooq.ResultQuery;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.regex.Pattern;

import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "ch.schlierelacht.admin")
public class ArchUnitTest {
    // usage of jOOQ's stream is dangerous since it must be closed manually or in a try-with-resources
    // this is often forgotten, therefore, we prohibit it entirely
    @ArchTest
    public static final ArchRule no_jooq_stream = noClasses().should().callMethodWhere(target(name("stream"))
                                                                                               .and(target(owner(assignableTo(ResultQuery.class)))));

    // always use DateUtil
    @ArchTest
    public static final ArchRule no_now_without_zone = noClasses()
            .should().callMethod(LocalDate.class, "now")
            .orShould().callMethod(LocalDateTime.class, "now")
            .orShould().callMethod(LocalTime.class, "now");

    @ArchTest
    public static final ArchRule no_classes_should_access_standard_streams = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
    @ArchTest
    public static final ArchRule no_classes_should_throw_generic_exceptions = GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
    @ArchTest
    public static final ArchRule no_classes_should_use_jodatime = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JODATIME;
    @ArchTest
    public static final ArchRule no_classes_should_use_java_util_logging = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
    @ArchTest
    public static final ArchRule no_classes_should_use_field_injection = GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
    @ArchTest
    public static final ArchRule testClassesShouldResideInTheSamePackageAsImplementation = GeneralCodingRules.testClassesShouldResideInTheSamePackageAsImplementation();
    @ArchTest
    public static final ArchRule old_date_and_time_classes_should_not_be_used = GeneralCodingRules.OLD_DATE_AND_TIME_CLASSES_SHOULD_NOT_BE_USED;
    @ArchTest
    public static final ArchRule assertions_should_have_detail_message = GeneralCodingRules.ASSERTIONS_SHOULD_HAVE_DETAIL_MESSAGE;
    @ArchTest
    public static final ArchRule deprecated_api_should_not_be_used = GeneralCodingRules.DEPRECATED_API_SHOULD_NOT_BE_USED;

    // the public API is versioned per resource (see ch.schlierelacht.admin.rest package-info):
    // an endpoint mapped without a version would be impossible to change without breaking the
    // website's production branch or a released build of the mobile app
    @ArchTest
    public static final ArchRule rest_endpoints_should_be_versioned = classes()
            .that().areAnnotatedWith(RestController.class)
            .should(beMappedUnderAVersionedApiPath());

    private static final Pattern VERSIONED_API_PATH = Pattern.compile("^/api/v\\d+/.+");

    private static ArchCondition<JavaClass> beMappedUnderAVersionedApiPath() {
        return new ArchCondition<>("be mapped under a versioned API path (/api/v<n>/<resource>)") {
            @Override
            public void check(JavaClass endpoint, ConditionEvents events) {
                var mapping = endpoint.tryGetAnnotationOfType(RequestMapping.class);
                if (mapping.isEmpty()) {
                    events.add(SimpleConditionEvent.violated(endpoint,
                                                             endpoint.getFullName() + " has no class-level @RequestMapping"));
                    return;
                }

                var paths = mapping.get().value().length > 0 ? mapping.get().value() : mapping.get().path();
                if (paths.length == 0) {
                    events.add(SimpleConditionEvent.violated(endpoint,
                                                             endpoint.getFullName() + " declares no @RequestMapping path"));
                    return;
                }

                Arrays.stream(paths)
                      .filter(path -> !VERSIONED_API_PATH.matcher(path).matches())
                      .forEach(path -> events.add(SimpleConditionEvent.violated(endpoint,
                                                                                endpoint.getFullName() + " is mapped to " + path
                                                                                        + ", which is not a versioned API path")));
            }
        };
    }
}
