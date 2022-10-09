/*
 * This file is part of TrackWorkTime (TWT).
 *
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License 3.0 as published by
 * the Free Software Foundation.
 *
 * TWT is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License 3.0 for more details.
 *
 * You should have received a copy of the GNU General Public License 3.0
 * along with TWT. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.trackworktime;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static java.util.stream.Collectors.toList;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnitAccess;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import org.junit.BeforeClass;
import org.junit.Test;
import org.zephyrsoft.trackworktime.util.ForeignCall;

import java.util.List;
import java.util.regex.Pattern;

public class ArchTest {

    private static String appPackageName;
    private static JavaClasses appClasses;

    private static final Pattern TEST_LOCATIONS = Pattern.compile(".*/(debugUnitTest|unitTest|debugAndroidTest|androidTest)/.*");

    @BeforeClass
    public static void setup() {
        appPackageName = ArchTest.class.getPackageName();
        appClasses = new ClassFileImporter().withImportOption(l -> !TEST_LOCATIONS.matcher(l.toString()).matches()).importPackages(appPackageName);
    }

    @Test
    public void noTestClassesAreChecked() {
        noClasses().should().haveSimpleNameEndingWith("IT").orShould().haveSimpleNameEndingWith("Test").check(appClasses);
    }

    @Test
    public void activitiesAreNotAbused() {
        methods()
            .that()
            .areDeclaredInClassesThat()
            .haveNameMatching("^" + appPackageName + "\\..*Activity(\\$.+)?$")
            .should(new ActivityMethodsAreOnlyCalledLocally())
            .check(appClasses);
    }

    public static class ActivityMethodsAreOnlyCalledLocally extends ArchCondition<JavaMethod> {

        public ActivityMethodsAreOnlyCalledLocally() {
            this("Activity instance methods which are not annotated with @"
                + ForeignCall.class.getSimpleName() + " are not accessed from other classes");
        }

        public ActivityMethodsAreOnlyCalledLocally(String description, Object... args) {
            super(description, args);
        }

        @Override
        public void check(JavaMethod method, ConditionEvents events) {
            if (method.isAnnotatedWith(ForeignCall.class)) {
                events.add(SimpleConditionEvent.satisfied(method, "Method " + method
                    + " is annotated with @" + ForeignCall.class.getSimpleName()));
                return;
            }

            if (method.getModifiers().contains(JavaModifier.STATIC)) {
                events.add(SimpleConditionEvent.satisfied(method, "Method " + method
                    + " is static"));
                return;
            }

            List<JavaCodeUnitAccess<?>> accesses = method.getAccessesToSelf().stream()
                .filter(a -> !getTopLevelClass(a.getOriginOwner())
                    .equals(getTopLevelClass(method.getOwner())))
                .collect(toList());

            if (accesses.isEmpty()) {
                events.add(SimpleConditionEvent.satisfied(method, "Method " + method
                    + " is accessed from within the same class"));
                return;
            }
            events.add(SimpleConditionEvent.violated(method, "Method " + method
                + " is accessed from a method from another class and is not annotated with @"
                + ForeignCall.class.getSimpleName() + ": " + accesses));
        }

        private static JavaClass getTopLevelClass(JavaClass javaClass) {
            JavaClass result = javaClass;
            while (result.getEnclosingClass().isPresent()) {
                result = result.getEnclosingClass().get();
            }
            return result;
        }
    }

}
