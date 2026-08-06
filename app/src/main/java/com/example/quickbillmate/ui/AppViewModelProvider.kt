package com.example.quickbillmate.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.quickbillmate.QuickBillMateApp
import com.example.quickbillmate.ui.contacts.ContactsImportViewModel
import com.example.quickbillmate.ui.customers.CustomersViewModel
import com.example.quickbillmate.ui.editor.EditorViewModel
import com.example.quickbillmate.ui.home.HomeViewModel
import com.example.quickbillmate.ui.presets.PresetEditorViewModel
import com.example.quickbillmate.ui.presets.PresetsViewModel
import com.example.quickbillmate.ui.products.ProductsViewModel
import com.example.quickbillmate.ui.settings.SettingsViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            HomeViewModel(app().repository)
        }
        initializer {
            EditorViewModel(app(), app().repository)
        }
        initializer {
            ProductsViewModel(app(), app().repository)
        }
        initializer {
            CustomersViewModel(app().repository)
        }
        initializer {
            ContactsImportViewModel(app(), app().repository)
        }
        initializer {
            SettingsViewModel(app(), app().repository)
        }
        initializer {
            PresetsViewModel(app(), app().repository)
        }
        initializer {
            PresetEditorViewModel(app().repository)
        }
    }

    private fun CreationExtras.app(): QuickBillMateApp =
        this[APPLICATION_KEY] as QuickBillMateApp
}
