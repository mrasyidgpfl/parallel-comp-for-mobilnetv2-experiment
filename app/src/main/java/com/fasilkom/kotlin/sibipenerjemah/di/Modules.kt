package com.fasilkom.kotlin.sibipenerjemah.di

import com.example.sibinative.ui.GestureGenerationContract
import com.example.sibinative.ui.GestureGenerationPresenter
import com.fasilkom.kotlin.sibipenerjemah.service.TestingModelProvider
import com.fasilkom.kotlin.sibipenerjemah.service.ModelProviderInterface
import com.fasilkom.kotlin.sibipenerjemah.service.ModelProviderLite
import com.fasilkom.kotlin.sibipenerjemah.ui.camera.CameraActivity
import com.fasilkom.kotlin.sibipenerjemah.ui.camera.CameraContract
import com.fasilkom.kotlin.sibipenerjemah.ui.camera.CameraPresenter
import com.fasilkom.kotlin.sibipenerjemah.ui.homepage.HomepageActivity
import com.fasilkom.kotlin.sibipenerjemah.ui.homepage.HomepageContract
import com.fasilkom.kotlin.sibipenerjemah.ui.homepage.HomepagePresenter
import com.fasilkom.kotlin.sibipenerjemah.ui.signtotext.PickerActivity
import com.fasilkom.kotlin.sibipenerjemah.ui.signtotext.PickerContract
import com.fasilkom.kotlin.sibipenerjemah.ui.signtotext.PickerPresenter
import org.koin.dsl.module

val appModule = module {
    /* Change to :
    - ModelProvider() to use Tensorflow-Android
    - ModelProviderLite() to use Tensorflow-Lite
    - ModelProviderDummy() to use Tensorflow-lite that built for survey purpose */
    single<ModelProviderInterface> { TestingModelProvider() }

    factory<PickerContract.Presenter> { (view: PickerActivity) -> PickerPresenter(view, get()) }
    factory<CameraContract.Presenter> { (view: CameraActivity) -> CameraPresenter(view, get()) }
    factory<HomepageContract.Presenter> { (view: HomepageActivity) -> HomepagePresenter(view, get()) }

    factory<GestureGenerationContract.Presenter> { GestureGenerationPresenter() }
}