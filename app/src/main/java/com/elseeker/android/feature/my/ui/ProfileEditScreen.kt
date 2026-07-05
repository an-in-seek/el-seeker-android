package com.elseeker.android.feature.my.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elseeker.android.R
import com.elseeker.android.core.ui.ResourceContent

/** 프로필 수정 화면: 닉네임 편집. 저장 성공 시 [onSaved]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nickname by viewModel.nickname.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val context = LocalContext.current

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ProfileEditEvent.Saved -> {
                    Toast.makeText(context, context.getString(R.string.my_profile_edit_saved), Toast.LENGTH_SHORT).show()
                    onSaved()
                }
                is ProfileEditEvent.Error ->
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
                title = { Text(stringResource(R.string.my_profile_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { inner ->
        ResourceContent(
            resource = state,
            onRetry = viewModel::load,
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) { _ ->
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = viewModel::onNicknameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.my_profile_edit_nickname_label)) },
                    singleLine = true,
                    enabled = !saving,
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = viewModel::save,
                    enabled = !saving && nickname.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (saving) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    } else {
                        Text(stringResource(R.string.my_profile_edit_save))
                    }
                }
            }
        }
    }
}
