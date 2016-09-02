package com.keepburning.storm.injector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormInjector {

    private static final Logger LOG = LoggerFactory.getLogger(StormInjector.class);

    public static void injectField(Map stormConf, Object target, Class... factoryClasses) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Map<String, StormFactoryHolder> factoryMap = resolveFactories(stormConf, factoryClasses);
        injectField(stormConf, target, factoryMap);
    }

    private static void injectField(Map stormConf, Object target, Map<String, StormFactoryHolder> factoryMap) throws InvocationTargetException, IllegalAccessException {
        final Field[] fields = target.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Inject.class)) {
                final Class fieldType = field.getType();
                final boolean isNamed = field.isAnnotationPresent(Named.class);
                final String name = isNamed ? field.getAnnotation(Named.class).value() : field.getName();

                StormFactoryHolder holder = factoryMap.get(name);
                Object value;
                if (holder != null) {
                    value = holder.getMethod().invoke(holder.getFactoryObj(), null);
                } else if (stormConf.containsKey(name)) {
                    value = stormConf.get(name);
                } else {
                    throw new RuntimeException("Can't find factory method or stormConf with name; name=" + name);
                }

                if (value == null || fieldType.isAssignableFrom(value.getClass())) {
                    field.setAccessible(true);
                    field.set(target, value);

                    LOG.debug("Field was injected; target={}, fieldType={}, name={}, isNamed={}, value={}",
                            target.getClass().getName(), fieldType, name, isNamed, value);

                    if (value != null) {
                        injectField(stormConf, value, factoryMap);
                    }
                } else {
                    throw new RuntimeException("Can't assign value (type invalid); target=" + target.getClass().getName() +
                            ", expected=" + fieldType.getName() + ", actual=" + value.getClass().getName());
                }
            }
        }
    }

    private static Map<String, StormFactoryHolder> resolveFactories(Map stormConf, Class... factoryClasses) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        List<Object> factoryObjList = new LinkedList<>();

        for (Class factoryClass : factoryClasses) {
            Constructor[] ctors = factoryClass.getDeclaredConstructors();
            Constructor ctor = null;
            for (int i = 0; i < ctors.length; i++) {
                if (ctors[i].getGenericParameterTypes().length > 0) {
                    throw new RuntimeException("Factory class must have only default constructor. (no-argument constructor)");
                }

                ctor = ctors[i];
            }

            ctor.setAccessible(true);

            Object object = ctor.newInstance();
            factoryObjList.add(object);
        }

        Map<String, StormFactoryHolder> factoryMap = new HashMap<>();
        for (Object factoryObj : factoryObjList) {
            Method[] methods = factoryObj.getClass().getMethods();
            for (Method method : methods) {
                final Class returnType = method.getReturnType();
                final String methodName = method.getName();
                final int parameterCount = method.getParameterTypes().length;
                if (Modifier.isPublic(method.getModifiers())
                        && !returnType.equals(Void.TYPE)
                        && parameterCount == 0
                        && !isObjectMethod(method)) {

                    if (!factoryMap.containsKey(methodName)) {
                        factoryMap.put(methodName, new StormFactoryHolder(factoryObj, method));
                    } else {
                        StormFactoryHolder holder = factoryMap.get(methodName);
                        throw new RuntimeException("Factory method is already registered with same name; " +
                                "registered=" + holder.getFactoryObj().getClass().getName() + "." + holder.getMethod().getName() +
                                "conflicted=" + factoryObj.getClass().getName() + "." + methodName);
                    }
                }
            }
        }

        for (Object factoryObj : factoryObjList) {
            injectField(stormConf, factoryObj, factoryMap);
        }

        return factoryMap;
    }

    private static boolean isObjectMethod(Method method) {
        final String methodName = method.getName();
        final int parameterCount = method.getParameterTypes().length;

        return parameterCount==0 && ("toString".equals(methodName) || "hashCode".equals(methodName) || "getClass".equals(methodName));
    }

}
