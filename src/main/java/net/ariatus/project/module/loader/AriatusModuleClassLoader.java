package net.ariatus.project.module.loader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AriatusModuleClassLoader extends URLClassLoader {

    static {
        registerAsParallelCapable();
    }

    private final CopyOnWriteArrayList<DependencyLink> dependencies = new CopyOnWriteArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public AriatusModuleClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public void addDependency(String moduleId, AriatusModuleClassLoader classLoader) {
        ensureOpen();

        String id = normalizeId(moduleId);

        Objects.requireNonNull(classLoader, "classLoader");

        if (classLoader == this) {
            throw new IllegalArgumentException("Un ClassLoader no puede depender de sí mismo.");
        }

        if (classLoader.isClosed()) {
            throw new IllegalStateException("No se puede enlazar un ClassLoader cerrado: " + id);
        }

        for (DependencyLink dependency : dependencies) {
            if (dependency.moduleId().equals(id)) {
                if (dependency.classLoader() != classLoader) {
                    throw new IllegalStateException("La dependencia " + id + " ya apunta a otro ClassLoader.");
                }

                return;
            }
        }

        dependencies.add(new DependencyLink(id, classLoader));
    }

    public boolean removeDependency(String moduleId) {
        String id = normalizeId(moduleId);

        return dependencies.removeIf(dependency ->
                dependency.moduleId().equals(id)
        );
    }

    public boolean dependsOn(String moduleId) {
        String id = normalizeId(moduleId);

        return dependencies.stream()
                .anyMatch(dependency ->
                        dependency.moduleId().equals(id)
                );
    }

    public List<String> dependencyIds() {
        return dependencies.stream()
                .map(DependencyLink::moduleId)
                .toList();
    }

    public boolean isClosed() {
        return closed.get();
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);

            if (loaded != null) {
                return loaded;
            }

            if (closed.get()) {
                throw new ClassNotFoundException(
                        "El ClassLoader del módulo ya está cerrado: " + name
                );
            }

            loaded = loadFromParent(name);

            if (loaded == null) {
                loaded = loadFromSelf(name);
            }

            if (loaded == null) {
                loaded = loadFromDependencies(name);
            }

            if (loaded == null) {
                throw new ClassNotFoundException(name);
            }

            if (resolve && loaded.getClassLoader() == this) {
                resolveClass(loaded);
            }

            return loaded;
        }
    }

    @Override
    public void close() throws IOException {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        dependencies.clear();

        super.close();
    }

    private Class<?> loadFromParent(String name) {
        try {
            return getParent().loadClass(name);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private Class<?> loadFromSelf(String name) {
        try {
            return findClass(name);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private Class<?> loadFromDependencies(String name) {
        for (DependencyLink dependency : dependencies) {
            Class<?> type = dependency.classLoader().findExportedClass(name);

            if (type != null) {
                return type;
            }
        }

        return null;
    }

    private Class<?> findExportedClass(String name) {
        if (closed.get()) {
            return null;
        }

        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);

            if (loaded != null) {
                return loaded;
            }

            try {
                return findClass(name);
            } catch (ClassNotFoundException ignored) {
                return null;
            }
        }
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("El ClassLoader ya está cerrado.");
        }
    }

    private String normalizeId(String moduleId) {
        String id = Objects.requireNonNull(moduleId, "moduleId")
                .trim()
                .toLowerCase(Locale.ROOT);

        if (id.isEmpty()) {
            throw new IllegalArgumentException("moduleId no puede estar vacío.");
        }

        return id;
    }

    private record DependencyLink(
            String moduleId,
            AriatusModuleClassLoader classLoader
    ) {
    }
}