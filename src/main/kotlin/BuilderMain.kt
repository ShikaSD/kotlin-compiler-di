import dagger.Component

@TestScope
@Component(modules = [TestModuleInstance::class])
interface BuilderMain {
    @Component.Builder
    interface Builder {
        fun module(module: TestModuleInstance): Builder
        fun build(): BuilderMain
    }

    fun integer(): Int
}
