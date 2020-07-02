package com.coy.l2cache.cache.spi;

import com.coy.l2cache.util.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SPI服务加载器
 *
 * @author chenck
 * @date 2020/7/2 16:56
 */
public class ServiceLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceLoader.class);
    private static final String BASE_DIRECTORY = "META-INF/";
    private static final String INTERNAL_DIRECTORY = BASE_DIRECTORY + "l2cache/";
    private static final String DEFAULT_IDENTITY = RandomUtil.getUUID();

    // 缓存spi服务接口<spi_clazz, 扩展点集合>
    private static final ConcurrentMap<Class<?>, ServiceProvider> serviceMap = new ConcurrentHashMap<>();
    // 缓存具体对象Map
    private static final ConcurrentMap<IdentityUniqueKey, Object> cachedObjectMap = new ConcurrentHashMap<>();

    public static <T> T load(Class<T> clazz) {
        return load(clazz, null);
    }

    public static <T> T load(Class<T> clazz, String name) {
        return load(clazz, name, DEFAULT_IDENTITY, false);
    }

    public static <T> T load(Class<T> clazz, String name, boolean alwaysCreate) {
        return load(clazz, name, DEFAULT_IDENTITY, alwaysCreate);
    }

    /**
     * 获取或加载扩展点
     *
     * @param clazz        扩展点接口class，标注了@SPI的接口
     * @param name         指定的扩展点名称
     * @param identity     唯一标识
     * @param alwaysCreate 是否总是创建对象， true 是，false 否(默认)
     * @return T
     * @author chenck
     * @date 2020/7/2 17:51
     */
    public static <T> T load(Class<T> clazz, String name, String identity, boolean alwaysCreate) {
        try {
            // 获取clazz对应的扩展点
            ServiceProvider serviceProvider = getServiceProvider(clazz);
            if (StringUtils.isEmpty(name)) {
                // 未指定name，则加载默认的
                name = serviceProvider.defaultName;
            }
            // 获取服务定义
            ServiceDefinition definition = serviceProvider.serviceDefinitionMap.get(buildName(name));
            if (definition == null) {
                throw new IllegalStateException(
                        "Service loader could not load name:" + name + "  class:" + clazz.getName()
                                + "'s ServiceProvider from '" + BASE_DIRECTORY + "' or '"
                                + INTERNAL_DIRECTORY + "' It may be empty or does not exist.");
            }
            if (alwaysCreate) {
                // 实例化并强制转换为clazz表示的接口
                return clazz.cast(ClassLoaderUtil.newInstance(definition.classLoader, definition.clazz));
            }

            // 用来保证每个节点都是一个各自的对象
            IdentityUniqueKey uniqueKey = new IdentityUniqueKey(identity, definition);

            Object obj = cachedObjectMap.get(uniqueKey);
            if (obj != null) {
                return (T) obj;
            }
            synchronized (definition) {
                obj = cachedObjectMap.get(uniqueKey);
                if (obj != null) {
                    return (T) obj;
                }
                // 实例化并强制转换为clazz表示的接口
                T instance = clazz.cast(ClassLoaderUtil.newInstance(definition.classLoader, definition.clazz));
                cachedObjectMap.putIfAbsent(uniqueKey, instance);
                return instance;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Service loader could not load name:" + name
                    + " class:" + clazz.getName() + "'s ServiceProvider from '" + BASE_DIRECTORY
                    + "' or '" + INTERNAL_DIRECTORY + "' It may be empty or does not exist.");
        }
    }

    /**
     * 获取服务提供者-获取扩展点
     */
    private static ServiceProvider getServiceProvider(Class<?> clazz) {
        ServiceProvider serviceProvider = serviceMap.get(clazz);
        if (serviceProvider == null) {
            loadServiceProvider(clazz);
            serviceProvider = serviceMap.get(clazz);
        }
        return serviceProvider;
    }

    /**
     * 加载服务提供者
     */
    public static Set<String> loadServiceProvider(final Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("type == null");
        }
        if (!clazz.isInterface()) {
            throw new IllegalArgumentException(" type(" + clazz + ") is not interface!");
        }
        if (!clazz.isAnnotationPresent(SPI.class)) {
            throw new IllegalArgumentException("type(" + clazz + ") is not extension, because WITHOUT @"
                    + SPI.class.getSimpleName() + " Annotation!");
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // 收集扩展点URL
        final Set<URLDefinition> urlDefinitions = new HashSet<>();
        urlDefinitions.addAll(collectExtensionUrls(BASE_DIRECTORY + clazz.getName(), classLoader));
        urlDefinitions.addAll(collectExtensionUrls(INTERNAL_DIRECTORY + clazz.getName(), classLoader));

        // 解析扩展点URL
        final ConcurrentMap<String, ServiceDefinition> serviceDefinitionMap = new ConcurrentHashMap<>();
        for (URLDefinition urlDefinition : urlDefinitions) {
            serviceDefinitionMap.putAll(parse(urlDefinition));
        }
        if (serviceDefinitionMap.isEmpty()) {
            throw new IllegalStateException("Service loader could not load " + clazz.getName()
                    + "'s ServiceProvider from '" + BASE_DIRECTORY + "' or '"
                    + INTERNAL_DIRECTORY + "' It may be empty or does not exist.");
        }

        // 构建服务提供者
        SPI spi = clazz.getAnnotation(SPI.class);
        ServiceProvider serviceProvider = new ServiceProvider(clazz, spi.value(), serviceDefinitionMap);
        serviceMap.remove(clazz); // 先移除
        serviceMap.put(clazz, serviceProvider);
        return serviceDefinitionMap.keySet();
    }

    /**
     * 解析
     */
    private static Map<String, ServiceDefinition> parse(URLDefinition urlDefinition) {
        final Map<String, ServiceDefinition> nameClassMap = new HashMap<String, ServiceDefinition>();
        try {
            URL url = urlDefinition.uri.toURL();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"))) {
                while (true) {
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    }
                    int comment = line.indexOf('#');
                    if (comment >= 0) {
                        line = line.substring(0, comment);
                    }
                    line = line.trim();
                    if (line.length() == 0) {
                        continue;
                    }

                    int i = line.indexOf('=');
                    if (i > 0) {
                        // 格式: name=扩展点实现类全路径，转小写便于后续映射name
                        String name = buildName(line.substring(0, i).trim());
                        String clazz = line.substring(i + 1).trim();
                        nameClassMap.put(name, new ServiceDefinition(name, clazz, urlDefinition.classLoader));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("parse " + urlDefinition.uri + " error:" + e.getMessage(), e);
        }
        return nameClassMap;
    }

    private static String buildName(String name) {
        return name.toLowerCase();
    }

    /**
     * 收集扩展点URL
     */
    private static Set<URLDefinition> collectExtensionUrls(String resourceName, ClassLoader classLoader) {
        try {
            final Enumeration<URL> configs;
            if (classLoader != null) {
                configs = classLoader.getResources(resourceName);
            } else {
                configs = ClassLoader.getSystemResources(resourceName);
            }

            Set<URLDefinition> urlDefinitions = new HashSet<URLDefinition>();
            while (configs.hasMoreElements()) {
                URL url = configs.nextElement();
                final URI uri = url.toURI();

                ClassLoader highestClassLoader = findHighestReachableClassLoader(url, classLoader, resourceName);
                urlDefinitions.add(new URLDefinition(uri, highestClassLoader));
            }
            return urlDefinitions;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return Collections.emptySet();
    }

    /**
     * 查找最高可达类加载器
     */
    private static ClassLoader findHighestReachableClassLoader(URL url, ClassLoader classLoader, String resourceName) {
        if (classLoader.getParent() == null) {
            return classLoader;
        }

        ClassLoader highestClassLoader = classLoader;
        ClassLoader current = classLoader;
        while (current.getParent() != null) {
            ClassLoader parent = current.getParent();
            try {
                Enumeration<URL> resources = parent.getResources(resourceName);
                if (resources == null) {
                    continue;
                }
                while (resources.hasMoreElements()) {
                    URL resourceURL = resources.nextElement();
                    if (url.toURI().equals(resourceURL.toURI())) {
                        highestClassLoader = parent;
                    }
                }
            } catch (IOException | URISyntaxException ignore) {
            }
            current = current.getParent();
        }
        return highestClassLoader;
    }

    /**
     * URL定义
     */
    private static final class URLDefinition {
        private final URI uri;
        private final ClassLoader classLoader;

        private URLDefinition(URI url, ClassLoader classLoader) {
            this.uri = url;
            this.classLoader = classLoader;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            URLDefinition that = (URLDefinition) o;
            if (uri != null) {
                return uri.equals(that.uri);
            } else {
                return that.uri == null;
            }
        }

        @Override
        public int hashCode() {
            return uri != null ? uri.hashCode() : 0;
        }
    }

    /**
     * 服务定义
     */
    private static final class ServiceDefinition {

        private final String name;
        private final String clazz;
        private final ClassLoader classLoader;

        private ServiceDefinition(String name, String clazz, ClassLoader classLoader) {
            this.name = name;
            this.clazz = clazz;
            this.classLoader = classLoader;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ServiceDefinition that = (ServiceDefinition) o;
            if (name != null) {
                if (!name.equals(that.name)) {
                    return false;
                }
            } else {
                if (that.name != null) {
                    return false;
                }
            }

            if (clazz != null) {
                if (!clazz.equals(that.clazz)) {
                    return false;
                }
            } else {
                if (that.clazz != null) {
                    return false;
                }
            }

            if (classLoader != null) {
                return classLoader.equals(that.classLoader);
            } else {
                return that.classLoader == null;
            }
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (clazz != null ? clazz.hashCode() : 0);
            result = 31 * result + (classLoader != null ? classLoader.hashCode() : 0);
            return result;
        }

    }

    /**
     *
     */
    private static class IdentityUniqueKey {
        private String identity;
        private ServiceDefinition definition;

        public IdentityUniqueKey(String identity, ServiceDefinition definition) {
            this.identity = identity;
            this.definition = definition;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            IdentityUniqueKey that = (IdentityUniqueKey) o;
            if (identity != null) {
                if (!identity.equals(that.identity)) {
                    return false;
                }
            } else {
                if (that.identity != null) {
                    return false;
                }
            }
            if (definition != null) {
                return definition.equals(that.definition);
            } else {
                return that.definition == null;
            }
        }

        @Override
        public int hashCode() {
            int result = identity != null ? identity.hashCode() : 0;
            result = 31 * result + (definition != null ? definition.hashCode() : 0);
            return result;
        }
    }

    /**
     * 服务提供者
     * 服务表示扩展点实现类
     */
    private static final class ServiceProvider {
        private final Class<?> clazz;// 扩展接口类
        private final String defaultName;// 默认的扩展点
        private final ConcurrentMap<String, ServiceDefinition> serviceDefinitionMap;// 扩展点集合

        public ServiceProvider(Class<?> clazz, String defaultName, ConcurrentMap<String, ServiceDefinition> serviceDefinitionMap) {
            this.clazz = clazz;
            this.defaultName = defaultName;
            this.serviceDefinitionMap = serviceDefinitionMap;
        }
    }
}
