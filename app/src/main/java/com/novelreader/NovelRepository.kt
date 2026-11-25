package com.novelreader

import android.content.Context
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.File

data class Author(val name: String = "未知作者")
data class Book(
    val id: Int,
    val title: String,
    val authors: List<Author> = emptyList(),
    val formats: Map<String, String> = emptyMap()
)
data class SearchResponse(@SerializedName("results") val results: List<Book> = emptyList())

interface GutenbergApi {
    @GET("books/")
    suspend fun search(@Query("search") search: String, @Query("languages") languages: String = "zh"): SearchResponse
}

class NovelRepository(private val context: Context) {
    private val api = Retrofit.Builder().baseUrl("https://gutendex.com/")
        .addConverterFactory(GsonConverterFactory.create()).build().create(GutenbergApi::class.java)
    private val http = OkHttpClient()

    suspend fun search(query: String): List<Book> = api.search(query).results

    fun download(book: Book): Boolean {
        val candidates = book.formats.entries.filter { it.key.contains("text/plain") }.sortedByDescending { it.key.contains("charset=utf-8") }
        val url = candidates.firstOrNull()?.value ?: book.formats.entries.firstOrNull { it.key.contains(".txt") }?.value ?: return false
        val response = http.newCall(Request.Builder().url(url).build()).execute()
        if (!response.isSuccessful) return false
        val bytes = response.body?.bytes() ?: return false
        val dir = File(context.filesDir, "library").apply { mkdirs() }
        val safeName = "${book.id}-${book.title}".replace(Regex("[\\\\/:*?\"<>|]"), "_").take(100)
        File(dir, "$safeName.txt").writeBytes(bytes)
        return true
    }

    fun library(): List<File> = File(context.filesDir, "library").listFiles { file -> file.extension == "txt" }?.sortedBy { it.name }?.toList() ?: emptyList()
}
