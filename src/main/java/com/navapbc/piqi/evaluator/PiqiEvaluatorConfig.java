package com.navapbc.piqi.evaluator;

import com.navapbc.piqi.map.fhir.PiqiBaseMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

@Configuration
@Slf4j
public class PiqiEvaluatorConfig {

    @Value("${com.nava.piqi.evaluator.server.url}")
    private String serverUrl;

    @Value("${com.nava.piqi.evaluator.connect.timeout}")
    private int connectTimeout;

    @Value("${com.nava.piqi.evaluator.read.timeout}")
    private int readTimeout;

    @Bean
    public Set<PiqiBaseMapper> piqiMappers() {
        Set<PiqiBaseMapper> mappers = new HashSet<>();
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(PiqiBaseMapper.class));
        Set<org.springframework.beans.factory.config.BeanDefinition> beanDefinitions =
                scanner.findCandidateComponents("com.navapbc.piqi.map");
        for (org.springframework.beans.factory.config.BeanDefinition beanDefinition : beanDefinitions) {
            try {
                Class<?> subclass = Class.forName(beanDefinition.getBeanClassName());
                log.info("Found subclass=[{}]", subclass.getName());
                Object instance = subclass.getDeclaredConstructor().newInstance();
                mappers.add((PiqiBaseMapper) instance);
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                     InvocationTargetException e) {
                log.warn("Couldn't load class for mapper.", e);
            }
        }
        return mappers;
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(customRequestFactory());
        return restTemplate;
    }

    public HttpComponentsClientHttpRequestFactory customRequestFactory() {
        var requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        return requestFactory;
    }
}
