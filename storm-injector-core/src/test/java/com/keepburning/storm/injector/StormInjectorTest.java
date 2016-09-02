package com.keepburning.storm.injector;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Assert;
import org.junit.Test;

public class StormInjectorTest {

    static class TargetClass {
        @Inject
        private SubClass1 subClass1;

        @Inject
        @Named("storm.conf.target_class.name")
        private String name;

        @Inject
        private String factoryNameUpper;
    }

    static class SubClass1 {
        @Inject
        @Named("subClass1Name")
        private String name;

        @Inject
        private Long subClass1Value;

        private SubClass2 subClass2;
    }

    static class SubClass2 {
        private String name;
    }

    static class FactoryClass {

        @Inject
        @Named("storm.conf.factory.name")
        private String name;

        public SubClass1 subClass1() {
            return new SubClass1();
        }

        public String subClass1Name() {
            return "I'm SubClass1";
        }

        public Long subClass1Value() {
            return 10L;
        }

        public String factoryNameUpper() { return this.name.toUpperCase(); }

    }

    static class FactoryClassWithConflictedName {

        public SubClass1 subClass1() {
            return new SubClass1();
        }

    }

    static class FactoryClassWithInvalidType {

        public SubClass1 subClass1() {
            return new SubClass1();
        }

        public String subClass1Name() {
            return "I'm SubClass1";
        }

        public String subClass1Value() {
            return "Value";
        }

    }

    static class FactoryClass3 {

        public SubClass2 subClass2() {
            return new SubClass2();
        }

        public String subClass2Name() {
            return "I'm SubClass2";
        }

    }

    @Test
    public void testInjectField() throws Exception {
        Map<String, Object> testConf = new HashMap<>();
        testConf.put("storm.conf.target_class.name", "targetName");
        testConf.put("storm.conf.factory.name", "factoryName");

        TargetClass t = new TargetClass();
        StormInjector.injectField(testConf, t, FactoryClass.class, FactoryClass3.class);
        Assert.assertNotNull(t.subClass1);
        Assert.assertEquals("I'm SubClass1", t.subClass1.name);
        Assert.assertEquals("targetName", t.name);
        Assert.assertEquals("FACTORYNAME", t.factoryNameUpper);
    }

    @Test
    public void testConflictedFactoryMethod() throws Exception {
        Map<String, Object> testConf = new HashMap<>();
        TargetClass t = new TargetClass();
        try {
            StormInjector.injectField(testConf, t, FactoryClass.class, FactoryClassWithConflictedName.class);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        try {
            StormInjector.injectField(testConf, t, FactoryClass.class, FactoryClassWithConflictedName.class);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInvalidTypeFactoryMethod() throws Exception {
        Map<String, Object> testConf = new HashMap<>();
        TargetClass t = new TargetClass();
        try {
            StormInjector.injectField(testConf, t, FactoryClassWithInvalidType.class);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

}