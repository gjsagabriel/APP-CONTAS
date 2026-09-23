package com.example.ui.export

import android.net.Uri

sealed interface ExportUiState {
    object Idle : ExportUiState
    object Exporting : ExportUiState
    data class Success(val fileName: String, val uri: Uri) : ExportUiState
    data class Error(val message: String) : ExportUiState
}
