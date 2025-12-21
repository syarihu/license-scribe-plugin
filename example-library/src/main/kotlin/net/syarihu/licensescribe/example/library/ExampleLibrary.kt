package net.syarihu.licensescribe.example.library

/**
 * Example library class that uses Retrofit.
 * This module is used to test that transitive dependencies
 * (like okhttp from Retrofit) are included in license detection.
 */
class ExampleLibrary {
    fun getMessage(): String = "Hello from example-library!"
}
