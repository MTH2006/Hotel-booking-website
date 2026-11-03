package com.hotelbooking.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class TestRunner {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   HOTEL BOOKING SYSTEM - UNIT TEST SUITE        ");
        System.out.println("=================================================");

        Class<?>[] testClasses = new Class<?>[] {
            PureUnitTestSuite.class,
            BookingNotificationServiceTest.class,
            BookingChangeNotificationServiceTest.class,
            PaymentNotificationServiceTest.class
        };

        int totalTests = 0;
        int passedTests = 0;
        int failedTests = 0;
        List<String> failedDetails = new ArrayList<>();

        for (Class<?> testClass : testClasses) {
            System.out.println("\n[RUNNING TEST CLASS]: " + testClass.getSimpleName());
            for (Method method : testClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Test.class)) {
                    totalTests++;
                    String testName = testClass.getSimpleName() + "." + method.getName();
                    try {
                        Object instance = testClass.getDeclaredConstructor().newInstance();

                        for (Method m : testClass.getDeclaredMethods()) {
                            if (m.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class)) {
                                m.setAccessible(true);
                                m.invoke(instance);
                            }
                        }

                        method.setAccessible(true);
                        method.invoke(instance);
                        passedTests++;
                        System.out.println("  [PASS] " + method.getName());
                    } catch (Throwable t) {
                        failedTests++;
                        Throwable cause = t.getCause() != null ? t.getCause() : t;
                        System.out.println("  [FAIL] " + method.getName() + " -> " + cause.getMessage());
                        failedDetails.add(testName + ": " + cause.getMessage());
                    }
                }
            }
        }

        System.out.println("\n=================================================");
        System.out.println("                 TEST SUMMARY                    ");
        System.out.println("=================================================");
        System.out.println(" Total Tests Run : " + totalTests);
        System.out.println(" Passed Tests    : " + passedTests);
        System.out.println(" Failed Tests    : " + failedTests);

        if (failedTests > 0) {
            System.out.println("\nFailed tests breakdown:");
            for (String f : failedDetails) {
                System.out.println(" - " + f);
            }
            System.exit(1);
        } else {
            System.out.println("\nALL UNIT TESTS PASSED SUCCESSFULLY! (100% OK)");
            System.exit(0);
        }
    }
}
