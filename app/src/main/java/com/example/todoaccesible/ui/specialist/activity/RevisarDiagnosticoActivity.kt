package com.example.todoaccesible.ui.specialist.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.todoaccesible.databinding.ActivityRevisarDiagnosticoBinding

class RevisarDiagnosticoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRevisarDiagnosticoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRevisarDiagnosticoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }
}
