package com.github.vatarasov.autowiredmap;

import com.github.vatarasov.autowiredmap.AutowiredMapTest.ImplementationA1;
import com.github.vatarasov.autowiredmap.AutowiredMapTest.ImplementationA2;
import com.github.vatarasov.autowiredmap.AutowiredMapTest.ImplementationB1;
import com.github.vatarasov.autowiredmap.AutowiredMapTest.ImplementationB2;
import com.github.vatarasov.autowiredmap.AutowiredMapTest.ImplementationC1;
import com.github.vatarasov.autowiredmap.AutowiredMapTest.ImplementationC2;
import com.github.vatarasov.autowiredmap.AutowiredMapTest.ProxiedImplementation;
import com.github.vatarasov.autowiredmap.AutowiredMapTest.ProxyBeanPostProcessor;
import com.github.vatarasov.autowiredmap.AutowiredMapTest.WithAutowiredMaps;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author vtarasov
 * @since 09.03.2020
 */
@SpringBootTest(classes = {
    AutowiredMapAnnotationBeanPostProcessor.class,
    WithAutowiredMaps.class,
    ImplementationA1.class, ImplementationA2.class,
    ImplementationB1.class, ImplementationB2.class,
    ImplementationC1.class, ImplementationC2.class,
    ProxiedImplementation.class, ProxyBeanPostProcessor.class
})
public class AutowiredMapTest {

    @Component
    static class ProxyBeanPostProcessor implements BeanPostProcessor {
        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            if ("autowiredMapTest.ProxiedImplementation".equals(beanName)) {
                return Proxy.newProxyInstance(ProxiedInterface.class.getClassLoader(), new Class<?>[] { ProxiedInterface.class },
                    (proxy, method, args) -> {
                        Object result = method.invoke(bean, args);
                        if ("getName".equals(method.getName())) {
                            return "proxied" + result;
                        }
                        return result;
                    });
            }
            return bean;
        }
    }

    private interface InterfaceA {}
    private interface InterfaceB {}
    private interface InterfaceC {}

    private interface ProxiedInterface {
        String getName();
    }

    @MapEntryComponent(map = "mapA", key = "a1")
    static class ImplementationA1 implements InterfaceA {}

    @MapEntryComponent(map = "mapA", key = "a2")
    static class ImplementationA2 implements InterfaceA {}

    @MapEntryComponent(map = "mapB", key = "b1")
    static class ImplementationB1 implements InterfaceB {}

    @MapEntryComponent(map = "mapB", key = "b2")
    static class ImplementationB2 implements InterfaceB {}

    @MapEntryComponent(map = "mapB2", key = "b3")
    static class ImplementationB3 implements InterfaceB {}

    @Component
    static class ImplementationC1 implements InterfaceC {}

    @Component
    static class ImplementationC2 implements InterfaceC {}

    @MapEntryComponent(map = "proxyMap", key = "proxyKey")
    static class ProxiedImplementation implements ProxiedInterface {
        @Override
        public String getName() {
            return "impl";
        }
    }

    @Component
    static class WithAutowiredMaps {

        @AutowiredMap(name = "mapA")
        private Map<String, InterfaceA> mapA1;

        @AutowiredMap(name = "mapA")
        private Map<String, InterfaceA> mapA2;

        @AutowiredMap(name = "mapA")
        private Map<String, InterfaceA> mapA3;

        @AutowiredMap(name = "mapB")
        private Map<String, InterfaceB> mapB1;

        @Autowired
        private Map<String, InterfaceB> mapB2;

        @AutowiredMap(name = "mapC")
        private Map<String, InterfaceC> mapC1;

        @Autowired
        private Map<String, InterfaceC> mapC2;

        @AutowiredMap(name = "proxyMap")
        private Map<String, ProxiedInterface> proxyMap;

        @Autowired
        WithAutowiredMaps(Map<String, InterfaceA> mapA2) {
            this.mapA2 = mapA2;
        }

        @Autowired
        void setMapA3(Map<String, InterfaceA> mapA3) {
            this.mapA3 = mapA3;
        }
    }

    @Autowired
    private WithAutowiredMaps withAutowiredMaps;

    @Autowired
    private ImplementationA1 implementationA1;

    @Autowired
    private ImplementationA2 implementationA2;

    @Autowired
    private ImplementationB1 implementationB1;

    @Autowired
    private ImplementationB2 implementationB2;

    @Autowired
    private ImplementationC1 implementationC1;

    @Autowired
    private ImplementationC2 implementationC2;

    @Test
    public void shouldUseAnnotatedKeysForInjectedIntoFieldMap() {
        assertEquals(withAutowiredMaps.mapA1.size(), 2);
        assertEquals(withAutowiredMaps.mapA1.get("a1"), implementationA1);
        assertEquals(withAutowiredMaps.mapA1.get("a2"), implementationA2);
    }

    @Test
    public void shouldUseAnnotatedKeysForInjectedIntoConstructorMap() {
        assertEquals(withAutowiredMaps.mapA2.size(), 2);
        assertEquals(withAutowiredMaps.mapA2.get("a1"), implementationA1);
        assertEquals(withAutowiredMaps.mapA2.get("a2"), implementationA2);
    }

    @Test
    public void shouldUseAnnotatedKeysForInjectedIntoSetterMap() {
        assertEquals(withAutowiredMaps.mapA3.size(), 2);
        assertEquals(withAutowiredMaps.mapA3.get("a1"), implementationA1);
        assertEquals(withAutowiredMaps.mapA3.get("a2"), implementationA2);
    }

    @Test
    public void shouldUseAnnotatedKeysForAnotherInjectedMap() {
        assertEquals(withAutowiredMaps.mapB1.size(), 2);
        assertEquals(withAutowiredMaps.mapB1.get("b1"), implementationB1);
        assertEquals(withAutowiredMaps.mapB1.get("b2"), implementationB2);
    }

    @Test
    public void shouldUseDefaultKeysForDefaultInjectedMap() {
        assertEquals(withAutowiredMaps.mapB2.size(), 2);
        assertEquals(withAutowiredMaps.mapB2.get("autowiredMapTest.ImplementationB1"), implementationB1);
        assertEquals(withAutowiredMaps.mapB2.get("autowiredMapTest.ImplementationB2"), implementationB2);
    }

    @Test
    public void shouldClearAutowiredMapForDefaultInjectedEntries() {
        assertEquals(withAutowiredMaps.mapC1.size(), 0);
    }

    @Test
    public void shouldUseDefaultKeysForDefaultAutowiredMap() {
        assertEquals(withAutowiredMaps.mapC2.size(), 2);
        assertEquals(withAutowiredMaps.mapC2.get("autowiredMapTest.ImplementationC1"), implementationC1);
        assertEquals(withAutowiredMaps.mapC2.get("autowiredMapTest.ImplementationC2"), implementationC2);
    }

    @Test
    public void shouldUseAnnotatedKeysForInjectedProxy() {
        assertEquals(withAutowiredMaps.proxyMap.size(), 1);
        assertEquals(withAutowiredMaps.proxyMap.get("proxyKey").getName(), "proxiedimpl");
    }
}
