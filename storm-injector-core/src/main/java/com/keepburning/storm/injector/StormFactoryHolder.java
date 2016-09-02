package com.keepburning.storm.injector;

import java.lang.reflect.Method;

class StormFactoryHolder {

    private final Object factoryObj;
    private final Method method;

    public StormFactoryHolder(Object factoryObj, Method method) {
        this.factoryObj = factoryObj;
        this.method = method;
    }

    public Object getFactoryObj() {
        return factoryObj;
    }

    public Method getMethod() {
        return method;
    }

    @Override
    public String toString() {
        return "StormFactoryHolder{" +
                "factoryObj=" + factoryObj +
                ", method=" + method +
                '}';
    }

}
