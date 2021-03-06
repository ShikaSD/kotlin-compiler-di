package me.shika.di.dagger.renderer.creator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.KModifier.LATEINIT
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import me.shika.di.dagger.renderer.asString
import me.shika.di.dagger.renderer.dsl.companionObject
import me.shika.di.dagger.renderer.dsl.function
import me.shika.di.dagger.renderer.dsl.nestedInterface
import me.shika.di.dagger.renderer.parameterName
import me.shika.di.dagger.renderer.typeName
import me.shika.di.model.Key

class DefaultBuilderRenderer(
    private val originalComponentClassName: ClassName,
    private val componentClassName: ClassName,
    private val constructorParams: List<Key>,
    private val builder: TypeSpec.Builder
) {
    private val builderClassName = componentClassName.nestedClass(BUILDER_IMPL_NAME)

    fun render() {
        builder.apply {
            builderClass()
            builderPublicMethod()
        }
    }

    private fun TypeSpec.Builder.builderClass() {
        nestedInterface(BUILDER_IMPL_NAME) {
            addAnnotation(DAGGER_ANNOTATION_NAME)
            val paramToProperty = constructorParams.associateWith {
                PropertySpec.builder(it.parameterName(), it.type.typeName()!!, PRIVATE, LATEINIT)
                    .mutable(true)
                    .build()
            }
            paramToProperty.values.forEach {
                createMethod(it)
            }

            function("build") {
                addModifiers(ABSTRACT)
                returns(componentClassName)
            }
        }
    }

    private fun TypeSpec.Builder.createMethod(param: PropertySpec) {
        val name = param.type.asString().decapitalize()
        function(name) {
            addModifiers(ABSTRACT)
            returns(builderClassName)
            addParameter(ParameterSpec.builder(name, param.type).build())
        }
    }

    private fun TypeSpec.Builder.builderPublicMethod() {
        companionObject {
            function("builder") {
                addAnnotation(JvmStatic::class)
                returns(builderClassName)
                addCode("return %M(%T::class.java)", DAGGER_BUILDER_NAME, builderClassName)
            }
        }
    }

    companion object {
        private const val BUILDER_IMPL_NAME = "Builder"
        private val DAGGER_BUILDER_NAME = MemberName("dagger.Dagger", "builder")
        private val DAGGER_ANNOTATION_NAME = ClassName("dagger", "Component", "Builder")
    }
}
