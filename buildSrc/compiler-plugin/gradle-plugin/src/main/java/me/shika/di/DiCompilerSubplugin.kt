package me.shika.di

import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

// @AutoService(KotlinGradleSubplugin::class)
class DiCompilerSubplugin: KotlinGradleSubplugin<KotlinCompile> {
    override fun apply(
        project: Project,
        kotlinCompile: KotlinCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?
    ): List<SubpluginOption> {
        val extension = project.extensions.findByType(DiCompilerExtension::class.java) ?: DiCompilerExtension()
        kotlinCompile.usePreciseJavaTracking = false

        val sourceSetName = if (variantData != null) {
            // Lol
            variantData.javaClass.getMethod("getName").run {
                isAccessible = true
                invoke(variantData) as String
            }
        } else {
            kotlinCompilation?.compilationName ?: "src"
        }

        val sources = File(project.buildDir, "generated/source/di-compiler/$sourceSetName/")
        kotlinCompilation?.allKotlinSourceSets?.forEach {
            it.kotlin.srcDir(sources)
            it.kotlin.exclude { it.file.startsWith(sources) }
        }

        // Lol #2
        variantData?.javaClass?.methods?.first { it.name =="addJavaSourceFoldersToModel" }?.apply {
            isAccessible = true
            invoke(variantData, sources)
        }

        return listOf(
            SubpluginOption(
                key = "enabled",
                value = extension.enabled.toString()
            ),
            SubpluginOption(
                key = "sources",
                value = sources.absolutePath
            )
        )
    }

    override fun getCompilerPluginId(): String = "dagger-compiler-plugin"

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(
            groupId = "me.shika.di",
            artifactId = "dagger-compiler-plugin",
            version = "0.0.2-preview-r2"
        )

    override fun isApplicable(project: Project, task: AbstractCompile): Boolean =
        project.plugins.hasPlugin(DiCompilerPlugin::class.java)
}
