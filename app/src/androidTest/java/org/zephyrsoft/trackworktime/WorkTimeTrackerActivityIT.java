package org.zephyrsoft.trackworktime;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.IdlingPolicies;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.zephyrsoft.trackworktime.weektimes.WeekTimesView;

import java.util.concurrent.TimeUnit;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class WorkTimeTrackerActivityIT {

    @Rule
    public RuleChain rules = RuleChain.outerRule(new ClearAppDataRule())
        .around(new ActivityScenarioRule<>(WorkTimeTrackerActivity.class));

    @BeforeClass
    public static void beforeClass() {
        // let's be generous, Github Actions are slow
        IdlingPolicies.setMasterPolicyTimeout(90, TimeUnit.SECONDS);
        IdlingPolicies.setIdlingResourceTimeout(90, TimeUnit.SECONDS);
    }

    @Test
    public void workTimeTrackerActivityIT() {
        ViewInteraction notNowButton = onView(
            allOf(withId(android.R.id.button2), withText(R.string.notNow),
                childAtPosition(
                    childAtPosition(
                        withId(androidx.appcompat.R.id.buttonPanel),
                        0),
                    2)));
        notNowButton.withFailureHandler((error, viewMatcher) -> {
            // do nothing in case the button cannot be found
            // (the dialog is not displayed on every start of the app)
        });
        notNowButton.perform(scrollTo(), click());

        ViewInteraction totalWorked1 = onView(
            allOf(withId(R.id.totalWorked),
                withParent(allOf(withId(R.id.totalRow),
                    withParent(allOf(withId(R.id.weekTable),
                        withParent(allOf(withClassName(equalTo(WeekTimesView.class.getName())),
                            childAtPosition(withClassName(containsString("RecyclerView")), 1)))))))));
        totalWorked1.check(matches(withText("00:00")));

        ViewInteraction weekTimesView = onView(
            childAtPosition(
                childAtPosition(
                    withId(R.id.week),
                    0),
                1));
        weekTimesView.perform(scrollTo(), click());

        ViewInteraction menuButton = onView(
            allOf(childAtPosition(
                    childAtPosition(
                        withId(androidx.appcompat.R.id.action_bar),
                        2),
                    0),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        menuButton.perform(click());

        ViewInteraction newPeriodMenuItem = onView(
            allOf(withText(R.string.newPeriod),
                childAtPosition(
                    childAtPosition(
                        withId(androidx.appcompat.R.id.content),
                        0),
                    0),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        newPeriodMenuItem.perform(click());

        ViewInteraction saveNewPeriodButton = onView(
            allOf(withId(R.id.save), withText(R.string.saveChanges),
                childAtPosition(
                    childAtPosition(
                        withClassName(is("android.widget.LinearLayout")),
                        0),
                    1),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        saveNewPeriodButton.perform(click());

        ViewInteraction backToMainActivity = onView(
            allOf(childAtPosition(
                    allOf(withId(androidx.appcompat.R.id.action_bar),
                        childAtPosition(
                            withId(androidx.appcompat.R.id.action_bar_container),
                            0)),
                    1),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        backToMainActivity.perform(click());

        ViewInteraction totalWorked2 = onView(
            allOf(withId(R.id.totalWorked),
                withParent(allOf(withId(R.id.totalRow),
                    withParent(allOf(withId(R.id.weekTable),
                        withParent(allOf(withClassName(equalTo(WeekTimesView.class.getName())),
                            childAtPosition(withClassName(containsString("RecyclerView")), 1)))))))));
        totalWorked2.check(matches(withText("00:01")));
    }

    private static Matcher<View> childAtPosition(
        final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                    && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
