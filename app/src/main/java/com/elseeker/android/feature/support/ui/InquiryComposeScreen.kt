package com.elseeker.android.feature.support.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elseeker.android.R
import com.elseeker.android.feature.support.data.InquiryCategory

/** 문의 작성/수정 화면: 카테고리 선택 + 제목 + 내용. 제출 성공 시 [onCreated]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InquiryComposeScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InquiryComposeViewModel = hiltViewModel(),
) {
    val category by viewModel.category.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val content by viewModel.content.collectAsStateWithLifecycle()
    val submitting by viewModel.submitting.collectAsStateWithLifecycle()
    val prefilling by viewModel.prefilling.collectAsStateWithLifecycle()
    val canSubmit by viewModel.canSubmit.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val editable = !submitting && !prefilling

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                InquiryComposeEvent.Created -> {
                    val messageRes = if (viewModel.isEdit) {
                        R.string.support_inquiry_updated
                    } else {
                        R.string.support_inquiry_created
                    }
                    Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
                    onCreated()
                }
                is InquiryComposeEvent.Error ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                expandedHeight = 48.dp,
                title = {
                    Text(
                        stringResource(
                            if (viewModel.isEdit) R.string.support_inquiry_edit_title
                            else R.string.support_inquiry_compose_title,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
        ) {
            CategoryDropdown(
                selected = category,
                onSelect = viewModel::onCategoryChange,
                enabled = editable,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = title,
                onValueChange = viewModel::onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.support_inquiry_title_label)) },
                singleLine = true,
                enabled = editable,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = content,
                onValueChange = viewModel::onContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                label = { Text(stringResource(R.string.support_inquiry_content_label)) },
                enabled = editable,
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = viewModel::submit,
                enabled = editable && canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (submitting) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text(
                        stringResource(
                            if (viewModel.isEdit) R.string.support_inquiry_update_submit
                            else R.string.support_inquiry_submit,
                        ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selected: InquiryCategory,
    onSelect: (InquiryCategory) -> Unit,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(
            onClick = { if (enabled) expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(selected.displayLabel(), modifier = Modifier.fillMaxWidth())
            Icon(Icons.Default.ArrowDropDown, contentDescription = stringResource(R.string.support_inquiry_category_desc))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            InquiryCategory.entries.forEach { c ->
                DropdownMenuItem(
                    text = { Text(c.displayLabel()) },
                    onClick = {
                        onSelect(c)
                        expanded = false
                    },
                )
            }
        }
    }
}
