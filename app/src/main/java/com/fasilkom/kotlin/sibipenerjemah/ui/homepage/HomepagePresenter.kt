package com.fasilkom.kotlin.sibipenerjemah.ui.homepage

import com.fasilkom.kotlin.sibipenerjemah.service.ModelProviderInterface

class HomepagePresenter(
    val view: HomepageActivity,
    private val modelProvider: ModelProviderInterface
): HomepageContract.Presenter {

    override fun loadClassifier() {
        if (!modelProvider.isClassifierLoaded()) {
            modelProvider.loadClassifier(view.applicationContext, view)
        }
    }
}