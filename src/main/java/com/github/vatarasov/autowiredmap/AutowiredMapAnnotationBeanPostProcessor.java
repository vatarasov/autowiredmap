package com.github.vatarasov.autowiredmap;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @author vtarasov
 * @since 08.03.2020
 */
@Component
public class AutowiredMapAnnotationBeanPostProcessor implements BeanPostProcessor {
    private static class BeanMapFieldInfo {
        private final String mapName;
        private final Field beanField;

        BeanMapFieldInfo(String mapName, Field beanField) {
            this.mapName = mapName;
            this.beanField = beanField;
        }
    }

    private Map<String, List<BeanMapFieldInfo>> beanMapFieldInfos = new HashMap<>();
    private Map<String, Map<String, String>> beanEntryKeyByBeanByMap = new HashMap<>();

    private BeanFactory beanFactory;

    @Autowired
    public AutowiredMapAnnotationBeanPostProcessor(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();

        Field[] beanFields = beanClass.getDeclaredFields();
        for (Field beanField : beanFields) {
            Optional.ofNullable(beanField.getDeclaredAnnotation(AutowiredMap.class))
                .ifPresent(autowiredMapAnnotation -> {
                    beanMapFieldInfos.putIfAbsent(beanName, new ArrayList<>());
                    beanMapFieldInfos.get(beanName).add(new BeanMapFieldInfo(autowiredMapAnnotation.name(), beanField));
                });
        }

        MapEntryComponent[] mapEntryComponentAnnotations = beanClass.getDeclaredAnnotationsByType(MapEntryComponent.class);
        for (MapEntryComponent mapEntryComponentAnnotation : mapEntryComponentAnnotations) {
            beanEntryKeyByBeanByMap.putIfAbsent(mapEntryComponentAnnotation.map(), new HashMap<>());
            beanEntryKeyByBeanByMap.get(mapEntryComponentAnnotation.map()).put(beanName, mapEntryComponentAnnotation.key());
        }

        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Optional.ofNullable(beanMapFieldInfos.get(beanName)).ifPresent(mapInfos -> {
            Object mapBean = beanFactory.getBean(beanName);

            mapInfos.forEach(mapInfo -> {
                String mapName = mapInfo.mapName;

                Field mapField = mapInfo.beanField;
                mapField.setAccessible(true);
                Map<String, ?> map;
                try {
                    map = (Map<String, ?>) mapField.get(mapBean);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

                Map newMap = new HashMap();
                map.entrySet().stream()
                    .filter(mapEntry -> beanEntryKeyByBeanByMap.containsKey(mapName))
                    .forEach(mapEntry -> {
                        Map<String, String> beanEntryKeyByBean = beanEntryKeyByBeanByMap.get(mapName);
                        String beanEntryKey = beanEntryKeyByBean.get(mapEntry.getKey());
                        if (beanEntryKey != null) {
                            newMap.put(beanEntryKey, mapEntry.getValue());
                        }
                    });

                map.clear();
                map.putAll(newMap);
            });
        });
        return bean;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void handleContextRefreshedEvent() {
        beanMapFieldInfos.clear();
        beanEntryKeyByBeanByMap.clear();
    }
}
