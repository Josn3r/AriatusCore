package net.ariatus.project.module.resource;

@FunctionalInterface
public interface ResourceCloser<T> {

    void close(T resource) throws Exception;

}