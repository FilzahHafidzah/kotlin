package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.file.FileTree
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import org.jetbrains.kotlin.gradle.utils.isJavaFile
import org.jetbrains.kotlin.gradle.utils.isKotlinFile
import org.jetbrains.kotlin.gradle.utils.isParentOf
import java.io.File
import java.util.*

internal sealed class SourceRoots(
    val kotlinSourceFiles: List<File>,
    val kotlinCommonSourceFiles: List<File>
) {
    private companion object {
        fun dumpPaths(files: Iterable<File>): String =
                "[${files.map { it.canonicalPath }.sorted().joinToString(prefix = "\n\t", separator = ",\n\t")}]"

        fun Iterable<File>.toKotlinFileList(): List<File> = filter(File::isKotlinFile)
    }

    val allKotlinSourceFiles: List<File>
        get() = kotlinSourceFiles + kotlinCommonSourceFiles

    open fun log(taskName: String, logger: Logger) {
        logger.kotlinDebug { "$taskName source roots: ${dumpPaths(kotlinSourceFiles)}" }
        if (kotlinCommonSourceFiles.isNotEmpty()) {
            logger.kotlinDebug { "$taskName common source roots: ${dumpPaths(kotlinCommonSourceFiles)}" }
        }
    }

    class ForJvm(
        kotlinSourceFiles: List<File>,
        kotlinCommonSourceFiles: List<File>,
        val javaSourceRoots: Set<File>
    ) : SourceRoots(kotlinSourceFiles, kotlinCommonSourceFiles) {
        companion object {
            fun create(taskSource: FileTree, commonSources: Iterable<File>, sourceRoots: FilteringSourceRootsContainer): ForJvm = ForJvm(
                taskSource.toKotlinFileList(),
                commonSources.toKotlinFileList(),
                findRootsForSources(sourceRoots.sourceRoots, taskSource.filter(File::isJavaFile))
            )

            private fun findRootsForSources(allSourceRoots: Iterable<File>, sources: Iterable<File>): Set<File> {
                val resultRoots = HashSet<File>()
                val sourceDirs = sources.mapTo(HashSet()) { it.parentFile }

                for (sourceDir in sourceDirs) {
                    for (sourceRoot in allSourceRoots) {
                        if (sourceRoot.isParentOf(sourceDir)) {
                            resultRoots.add(sourceRoot)
                        }
                    }
                }

                return resultRoots
            }
        }

        override fun log(taskName: String, logger: Logger) {
            super.log(taskName, logger)
            logger.kotlinDebug { "$taskName java source roots: ${dumpPaths(javaSourceRoots)}" }
        }
    }

    class KotlinOnly(
        kotlinSourceFiles: List<File>,
        kotlinCommonSourceFiles: List<File>
    ) : SourceRoots(kotlinSourceFiles, kotlinCommonSourceFiles) {
        companion object {
            fun create(taskSource: FileTree, commonSources: Iterable<File>): KotlinOnly =
                KotlinOnly(taskSource.toKotlinFileList(), commonSources.toKotlinFileList())
        }
    }
}

internal class FilteringSourceRootsContainer(roots: List<File> = emptyList(), val filter: (File) -> Boolean = { true }) {
    private val mutableSourceRoots = roots.filterTo(mutableListOf(), filter)

    val sourceRoots: List<File>
        get() = mutableSourceRoots

    fun clear() {
        mutableSourceRoots.clear()
    }

    fun set(source: Any?): List<File> {
        clear()
        return add(source)
    }

    fun add(vararg sources: Any?): List<File> {
        val filteredDirs = mutableListOf<File>()
        for (source in sources) {
            when (source) {
                is SourceDirectorySet -> filteredDirs += source.srcDirs.filter { filter(it) }
                is File -> if (filter(source)) filteredDirs.add(source)
                is Collection<*> -> source.forEach { filteredDirs += add(it) }
                is Array<*> -> source.forEach { filteredDirs += add(it) }
            }
        }

        mutableSourceRoots += filteredDirs
        return filteredDirs
    }
}
