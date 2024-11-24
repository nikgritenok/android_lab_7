package com.example.mydialer

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import android.text.Editable
import android.text.TextWatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import android.content.SharedPreferences

data class Contact(
    val name: String,
    val phone: String,
    val type: String
)

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var contactsList: List<Contact>
    private lateinit var searchEditText: EditText
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Timber.plant(Timber.DebugTree())

        sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

        searchEditText = findViewById(R.id.et_search)
        val recyclerView = findViewById<RecyclerView>(R.id.rView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        contactAdapter = ContactAdapter()
        recyclerView.adapter = contactAdapter

        loadContacts("https://drive.google.com/uc?export=download&id=1-KO-9GA3NzSgIc1dkAsNm8Dqw0fuPxcR")

        val savedFilter = sharedPreferences.getString("SEARCH_FILTER", "") ?: ""
        searchEditText.setText(savedFilter)

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Не нужно ничего делать здесь
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = s.toString()
                Timber.d("Filtering for: $searchText")
                if (searchText.isEmpty()) {
                    contactAdapter.submitList(contactsList)  // Обновляем весь список
                } else {
                    val filteredContacts = contactsList.filter { contact ->
                        contact.name.contains(searchText, ignoreCase = true)
                    }
                    contactAdapter.submitList(filteredContacts)  // Обновляем только отфильтрованные данные
                }
            }

            override fun afterTextChanged(s: Editable?) {
                val searchText = s.toString()
                sharedPreferences.edit().putString("SEARCH_FILTER", searchText).apply()
            }
        })
    }

    private fun loadContacts(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    if (json != null) {
                        val contacts = parseContacts(json)
                        withContext(Dispatchers.Main) {
                            contactsList = contacts
                            val savedFilter = sharedPreferences.getString("SEARCH_FILTER", "") ?: ""
                            if (savedFilter.isNotEmpty()) {
                                val filteredContacts = contactsList.filter { contact ->
                                    contact.name.contains(savedFilter, ignoreCase = true)
                                }
                                contactAdapter.submitList(filteredContacts)
                            } else {
                                contactAdapter.submitList(contactsList)
                            }
                        }
                    }
                } else {
                    Timber.e("Failed to access file: ${response.message}")
                }
            } catch (e: IOException) {
                Timber.e(e, "Error loading contacts")
            }
        }
    }

    private fun parseContacts(json: String): List<Contact> {
        val listType = object : TypeToken<List<Contact>>() {}.type
        return Gson().fromJson(json, listType)
    }
}