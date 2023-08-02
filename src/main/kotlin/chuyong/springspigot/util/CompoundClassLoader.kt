package chuyong.springspigot.util

import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.*

/**
 * A ClassLoader implementation that iterates over a collection of other ClassLoaders until it finds everything it's looking for.
 *
 * @author David Ward &lt;[dward@jboss.org](mailto:dward@jboss.org)&gt; (C) 2011 Red Hat Inc.
 */
class CompoundClassLoader : ClassLoader {
    private val _loaders: Collection<ClassLoader>

    /**
     * Constructs a new CompoundClassLoader.
     *
     * @param loaders the loaders to iterate over
     */
    constructor(vararg loaders: ClassLoader) {
        _loaders = listOf(*loaders)
    }

    /**
     * Constructs a new CompoundClassLoader.
     *
     * @param loaders the loaders to iterate over
     */
    constructor(loaders: Collection<ClassLoader>) {
        _loaders = loaders
    }

    /**
     * {@inheritDoc}
     */
    override fun getResource(name: String): URL? {
        for (loader in _loaders) {
            if (loader != null) {
                val resource = loader.getResource(name)
                if (resource != null) {
                    return resource
                }
            }
        }
        return null
    }

    /**
     * {@inheritDoc}
     */
    override fun getResourceAsStream(name: String): InputStream? {
        for (loader in _loaders) {
            if (loader != null) {
                val `is` = loader.getResourceAsStream(name)
                if (`is` != null) {
                    return `is`
                }
            }
        }
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Throws(IOException::class)
    override fun getResources(name: String): Enumeration<URL> {
        val urls: MutableList<URL> = ArrayList()
        for (loader in _loaders) {
            if (loader != null) {
                try {
                    val resources = loader.getResources(name)
                    while (resources.hasMoreElements()) {
                        val resource = resources.nextElement()
                        if (resource != null && !urls.contains(resource)) {
                            urls.add(resource)
                        }
                    }
                } catch (ioe: IOException) {
                    // ignoring, but to keep checkstyle happy ("Must have at least one statement."):
                    ioe.message
                }
            }
        }
        return Collections.enumeration(urls)
    }

    /**
     * {@inheritDoc}
     */
    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String): Class<*> {
        for (loader in _loaders) {
            if (loader != null) {
                try {
                    return loader.loadClass(name)
                } catch (cnfe: ClassNotFoundException) {
                    // ignoring, but to keep checkstyle happy ("Must have at least one statement."):
                    cnfe.message
                }
            }
        }
        throw ClassNotFoundException()
    }

    /**
     * {@inheritDoc}
     */
    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        // loader.loadClass(name, resolve) is not visible!
        return loadClass(name)
    }

    override fun toString(): String {
        return String.format("CompoundClassloader %s", _loaders)
    }
}
