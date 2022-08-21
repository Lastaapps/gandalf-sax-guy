package cz.lastaapps.gandalfsaxguy

import org.koin.androidx.compose.viewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val module = module {
    viewModel { MainViewModel(get())}
    singleOf(::CounterStore)
}