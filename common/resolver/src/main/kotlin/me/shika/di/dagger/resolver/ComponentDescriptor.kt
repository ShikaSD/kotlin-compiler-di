package me.shika.di.dagger.resolver

import me.shika.di.COMPONENT_NOT_ABSTRACT
import me.shika.di.COMPONENT_TYPE_PARAMETER
import me.shika.di.COMPONENT_WITH_FACTORY_AND_BUILDER
import me.shika.di.COMPONENT_WITH_MULTIPLE_BUILDERS
import me.shika.di.COMPONENT_WITH_MULTIPLE_FACTORIES
import me.shika.di.dagger.resolver.creator.BuilderDescriptor
import me.shika.di.dagger.resolver.creator.CreatorDescriptor
import me.shika.di.dagger.resolver.creator.DefaultBuilderDescriptor
import me.shika.di.dagger.resolver.creator.FactoryDescriptor
import me.shika.di.model.Binding
import me.shika.di.model.Key
import me.shika.di.model.ResolveResult
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered

class DaggerComponentDescriptor(
    val definition: ClassDescriptor,

    val context: ResolverContext
) {
    var creatorDescriptor: CreatorDescriptor? = null
        private set

    lateinit var annotation: ComponentAnnotationDescriptor
        private set

    var parameters: List<Key> = emptyList()
        private set

    lateinit var graph: List<ResolveResult>
        private set

    init {
        parseDefinition()
    }

    private fun parseDefinition() {
        val componentAnnotation = definition.componentAnnotation()?.let {
            ComponentAnnotationDescriptor(context, it)
        } ?: return

        annotation = componentAnnotation
        val scopes = definition.scopeAnnotations()

        creatorDescriptor = definition.findCreator(componentAnnotation) ?: DefaultBuilderDescriptor()

//        val bindingResolvers = listOfNotNull(
//            ModuleBindingResolver(componentAnnotation, definition, context),
//            DependencyBindingResolver(componentAnnotation, definition, context),
//            creatorDescriptor?.let { CreatorInstanceBindingResolver(it) },
//            { listOf(componentBinding()) }
//        )

        parameters = listOfNotNull(
            componentAnnotation.moduleInstances.map { Key(it.defaultType) },
            componentAnnotation.dependencies.map { Key(it.defaultType) },
            creatorDescriptor?.instances?.map { it.key }
        ).flatten()
//
//        val endpointResolvers = listOf(
//            ProvisionEndpointResolver(componentAnnotation, definition, context),
//            InjectionEndpointResolver(componentAnnotation, definition, context)
//        )
//
//        graph = GraphBuilder(
//            context,
//            scopes,
//            endpointResolvers.flatMap { it() },
//            bindingResolvers.flatMap { it() }
//        ).build()
    }

    private fun ClassDescriptor.componentAnnotation() =
        when {
            modality != Modality.ABSTRACT || this is AnnotationDescriptor -> {
                report(context.trace) { COMPONENT_NOT_ABSTRACT.on(it) }
                null
            }
            declaredTypeParameters.isNotEmpty() -> {
                report(context.trace) { COMPONENT_TYPE_PARAMETER.on(it) }
                null
            }
            else -> annotations.findAnnotation(DAGGER_COMPONENT_ANNOTATION)
        }

    private fun ClassDescriptor.findCreator(componentAnnotation: ComponentAnnotationDescriptor): CreatorDescriptor? {
        val innerClasses = innerClasses()
        val factories = innerClasses.filter { it.annotations.hasAnnotation(DAGGER_FACTORY_ANNOTATION) }
        val builders = innerClasses.filter { it.annotations.hasAnnotation(DAGGER_BUILDER_ANNOTATION) }

        if (factories.size > 1) {
            report(context.trace) { COMPONENT_WITH_MULTIPLE_FACTORIES.on(it, factories) }
        }

        if (builders.size > 1) {
            report(context.trace) { COMPONENT_WITH_MULTIPLE_BUILDERS.on(it, builders) }
        }

        if (factories.isNotEmpty() && builders.isNotEmpty()) {
            report(context.trace) { COMPONENT_WITH_FACTORY_AND_BUILDER.on(it, factories, builders) }
        }

        return factories.firstOrNull()?.let { FactoryDescriptor(definition, it, componentAnnotation, context) }
            ?: builders.firstOrNull()?.let { BuilderDescriptor(definition, it, componentAnnotation, context) }
    }

    private fun componentBinding() = Binding(
        Key(definition.defaultType, emptyList()),
        emptyList(),
        Binding.Variation.Component(definition)
    )
}

private val DAGGER_COMPONENT_ANNOTATION = FqName("dagger.Component")
private val DAGGER_FACTORY_ANNOTATION = FqName("dagger.Component.Factory")
private val DAGGER_BUILDER_ANNOTATION = FqName("dagger.Component.Builder")

private fun ClassDescriptor.innerClasses() =
    unsubstitutedMemberScope.getDescriptorsFiltered(DescriptorKindFilter.CLASSIFIERS) { true }
        .filterIsInstance<ClassDescriptor>()
        .filter { it.kind == ClassKind.INTERFACE || (it.kind == ClassKind.CLASS && it.modality == Modality.ABSTRACT) }

fun ClassDescriptor.isComponent() =
    annotations.hasAnnotation(DAGGER_COMPONENT_ANNOTATION)
