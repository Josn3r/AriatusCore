package net.ariatus.project.module.loader;

import java.net.URL;
import java.net.URLClassLoader;

public class AriatusModuleClassLoader extends URLClassLoader {

    public AriatusModuleClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }
}