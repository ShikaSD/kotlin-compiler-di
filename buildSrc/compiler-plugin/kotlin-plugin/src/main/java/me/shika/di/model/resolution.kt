package me.shika.di.model

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor

data class GraphNode(val value: Binding, val dependencies: List<GraphNode>)

data class ResolveResult(val endpoint: Endpoint, val graph: List<GraphNode>)

sealed class Endpoint {
    abstract val source: FunctionDescriptor

    data class Exposed(override val source: FunctionDescriptor) : Endpoint()
    data class Injected(override val source: FunctionDescriptor, val value: Injectable) : Endpoint()

    val types get() = when (this) {
        is Injected -> value.types
        is Exposed -> listOf(source.returnType)
    }
}

sealed class Injectable {
    data class Setter(val cls: ClassDescriptor, val descriptor: FunctionDescriptor): Injectable()
    data class Property(val cls: ClassDescriptor, val descriptor: PropertyDescriptor): Injectable()

    val types get() = when(this) {
        is Setter -> descriptor.valueParameters.map { it.type }
        is Property -> listOf(descriptor.returnType)
    }
}

sealed class Binding {
    abstract val scopes: List<AnnotationDescriptor>

    data class InstanceFunction(
        val instance: ClassDescriptor,
        val descriptor: FunctionDescriptor,
        override val scopes: List<AnnotationDescriptor>
    ): Binding()

    data class StaticFunction(
        val descriptor: FunctionDescriptor,
        override val scopes: List<AnnotationDescriptor>
    ): Binding()

    data class Constructor(
        val descriptor: ClassConstructorDescriptor,
        override val scopes: List<AnnotationDescriptor>
    ): Binding()

    val resolvedDescriptor get() = when (this) {
        is InstanceFunction -> descriptor
        is StaticFunction -> descriptor
        is Constructor -> descriptor
    }

    val type get() = resolvedDescriptor.returnType
}