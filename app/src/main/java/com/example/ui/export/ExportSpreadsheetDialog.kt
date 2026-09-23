package com.example.ui.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.exporter.ExportPeriodScope
import com.example.domain.exporter.ExportTransactionScope
import com.example.domain.exporter.ExportTypeScope
import com.example.domain.exporter.SpreadsheetExportOptions
import com.example.domain.exporter.SpreadsheetExporter
import com.example.ui.MainViewModel

private const val XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSpreadsheetDialog(
    viewModel: MainViewModel,
    hasActiveFilters: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val exportUiState by viewModel.exportUiState.collectAsStateWithLifecycle()

    var transactionScope by remember {
        mutableStateOf(
            if (hasActiveFilters) ExportTransactionScope.FILTERED else ExportTransactionScope.ALL
        )
    }
    var periodScope by remember { mutableStateOf(ExportPeriodScope.ALL) }
    var typeScope by remember { mutableStateOf(ExportTypeScope.ALL) }
    var includeCards by remember { mutableStateOf(true) }

    val suggestedFileName = remember { SpreadsheetExporter.getSuggestedFileName() }

    // Launcher do seletor nativo do Android para salvar arquivos (CreateDocument)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(XLSX_MIME_TYPE)
    ) { uri: Uri? ->
        if (uri != null) {
            val options = SpreadsheetExportOptions(
                transactionScope = transactionScope,
                periodScope = periodScope,
                typeScope = typeScope,
                includeCardsAndInvoices = includeCards
            )
            viewModel.exportSpreadsheet(context, uri, options, suggestedFileName)
        }
        // Se o usuário cancelar a seleção (uri == null), não faz nada e não exibe erro indevido
    }

    // Modal de Opções de Exportação
    if (exportUiState is ExportUiState.Idle) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier.testTag("export_spreadsheet_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Cabeçalho
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(12.dp)
                                .size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Exportar Planilha",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Gere um arquivo Excel (.xlsx) compatível",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Seção 1: Escopo das movimentações (se houver filtros ativos)
                if (hasActiveFilters) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Escopo dos Dados",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { transactionScope = ExportTransactionScope.FILTERED }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = transactionScope == ExportTransactionScope.FILTERED,
                                        onClick = { transactionScope = ExportTransactionScope.FILTERED }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Apenas os resultados dos filtros atuais",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { transactionScope = ExportTransactionScope.ALL }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = transactionScope == ExportTransactionScope.ALL,
                                        onClick = { transactionScope = ExportTransactionScope.ALL }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Todas as movimentações cadastradas",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                // Seção 2: Período
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Período",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExportPeriodScope.values().forEach { scope ->
                            FilterChip(
                                selected = periodScope == scope,
                                onClick = { periodScope = scope },
                                label = { Text(scope.displayName, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Seção 3: Tipos de Lançamento
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tipos de Lançamento",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExportTypeScope.values().forEach { scope ->
                            FilterChip(
                                selected = typeScope == scope,
                                onClick = { typeScope = scope },
                                label = { Text(scope.displayName, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Seção 4: Incluir Cartões e Faturas
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cartões, Faturas e Parcelas",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Inclui abas adicionais com status dos cartões, limites e detalhamento de parcelas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = includeCards,
                            onCheckedChange = { includeCards = it },
                            modifier = Modifier.testTag("switch_include_cards")
                        )
                    }
                }

                // Botão de Ação Principal
                Button(
                    onClick = {
                        createDocumentLauncher.launch(suggestedFileName)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("button_choose_save_location")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Escolher Local e Salvar (.xlsx)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }

    // Estado: Gerando planilha
    if (exportUiState is ExportUiState.Exporting) {
        AlertDialog(
            onDismissRequest = { /* Bloqueado durante exportação */ },
            confirmButton = {},
            title = {
                Text(text = "Gerando Planilha", fontWeight = FontWeight.Bold)
            },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Text(
                        text = "Construindo o arquivo .xlsx e aplicando formatações...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        )
    }

    // Estado: Sucesso
    val successState = exportUiState as? ExportUiState.Success
    if (successState != null) {
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissExportState()
                onDismiss()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Planilha Exportada com Sucesso!",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "O arquivo foi gerado e salvo no local selecionado:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = successState.fileName,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Text(
                        text = "Compatível com Microsoft Excel, Google Sheets e LibreOffice Calc.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            shareFile(context, successState.uri, successState.fileName)
                        },
                        modifier = Modifier.testTag("button_share_spreadsheet")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartilhar")
                    }

                    Button(
                        onClick = {
                            openFile(context, successState.uri)
                        },
                        modifier = Modifier.testTag("button_open_spreadsheet")
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Abrir")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissExportState()
                        onDismiss()
                    }
                ) {
                    Text("Concluir")
                }
            }
        )
    }

    // Estado: Erro
    val errorState = exportUiState as? ExportUiState.Error
    if (errorState != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissExportState() },
            icon = {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(text = "Erro ao Exportar Planilha", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = errorState.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissExportState() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Entendi")
                }
            }
        )
    }
}

private fun openFile(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, XLSX_MIME_TYPE)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(
            context,
            "Nenhum aplicativo compatível com planilhas (.xlsx) encontrado.",
            Toast.LENGTH_LONG
        ).show()
    }
}

private fun shareFile(context: Context, uri: Uri, fileName: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = XLSX_MIME_TYPE
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Compartilhar Planilha"))
    } catch (_: Exception) {
        Toast.makeText(context, "Não foi possível compartilhar o arquivo.", Toast.LENGTH_SHORT).show()
    }
}
