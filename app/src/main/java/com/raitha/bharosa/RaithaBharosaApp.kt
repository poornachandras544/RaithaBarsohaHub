package com.raitha.bharosa

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Annotated with @HiltAndroidApp to trigger
 * Hilt's code generation and serve as the root dependency container.
 */
@HiltAndroidApp
class RaithaBharosaApp : Application()
