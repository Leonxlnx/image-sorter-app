package com.leonxlnx.imagesorter

import android.app.Application
import com.leonxlnx.imagesorter.data.FolderRepository
import com.leonxlnx.imagesorter.data.PhotoRepository
import com.leonxlnx.imagesorter.data.ReviewedRepository
import com.leonxlnx.imagesorter.data.SettingsRepository
import com.leonxlnx.imagesorter.data.SortActions

/**
 * Application entry point. Hosts the lightweight service-locator used across the app.
 *
 * Each repository is intentionally cheap so we eagerly construct them and expose them as
 * singletons. This keeps the rest of the codebase free of any DI framework.
 */
class ImageSorterApp : Application() {

    lateinit var photoRepository: PhotoRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var folderRepository: FolderRepository
        private set
    lateinit var reviewedRepository: ReviewedRepository
        private set
    lateinit var sortActions: SortActions
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(applicationContext)
        folderRepository = FolderRepository(applicationContext)
        reviewedRepository = ReviewedRepository(applicationContext)
        photoRepository = PhotoRepository(applicationContext)
        sortActions = SortActions(applicationContext, folderRepository, reviewedRepository)
    }
}
