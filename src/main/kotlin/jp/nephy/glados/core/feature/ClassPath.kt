package jp.nephy.glados.core.feature

import java.io.File
import java.net.JarURLConnection
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

class ClassPath(val packageName: String) {
    val classLoader = Thread.currentThread().contextClassLoader!!
    val classNamePattern = "([A-Za-z]+(\\d)?)\\.class".toRegex()
    val packageSeparator = '.'
    val jarPathSeparator = '/'
    val fileSystemPathSeparator = File.separatorChar
    val jarResourceName = packageName.replace('.', jarPathSeparator)
    val fileSystemResourceName = packageName.replace('.', fileSystemPathSeparator)

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> classes(): Sequence<Class<T>> {
        val root = classLoader.getResource(fileSystemResourceName)
                ?: classLoader.getResource(jarResourceName)
                ?: return emptySequence()

        return when (root.protocol) {
            "file" -> {
                val paths = arrayListOf<Path>()
                Files.walkFileTree(Paths.get(root.toURI()), object: SimpleFileVisitor<Path>() {
                    override fun visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult {
                        paths.add(path)
                        return FileVisitResult.CONTINUE
                    }
                })

                paths.asSequence().map {
                    ClassEntry(it.toString(), it.fileName.toString())
                }.loadClasses(fileSystemResourceName, fileSystemPathSeparator)
            }
            "jar" -> {
                (root.openConnection() as JarURLConnection).jarFile.use {
                    it.entries().toList()
                }.asSequence().filter {
                    it.name.startsWith(jarResourceName)
                }.map {
                    ClassEntry(it.name, it.name.split(jarPathSeparator).last())
                }.loadClasses<T>(jarResourceName, jarPathSeparator)
            }
            else -> throw UnsupportedOperationException("Unknown procotol: ${root.protocol}")
        }.sortedBy { it.canonicalName }
    }

    data class ClassEntry(val path: String, val filename: String)

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> Sequence<ClassEntry>.loadClasses(resourceName: String, pathSeparator: Char): Sequence<Class<T>> {
        return filter { classNamePattern.containsMatchIn(it.filename) }
                .asSequence()
                .map { "$packageName${it.path.split(resourceName).last().replace(pathSeparator, packageSeparator).removeSuffix(".class")}" }
                .map { classLoader.loadClass(it) }
                .filter { it.superclass == T::class.java }
                .map { it as Class<T> }
    }
}
