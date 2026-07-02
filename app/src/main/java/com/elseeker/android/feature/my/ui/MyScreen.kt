package com.elseeker.android.feature.my.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.elseeker.android.R
import com.elseeker.android.core.ui.ResourceContent
import com.elseeker.android.feature.auth.data.AuthMeResponse

/**
 * 마이 탭(허브): 프로필 + 관리 진입점(프로필 수정·연동 관리·내 메모·1:1 문의) + 로그아웃/회원 탈퇴.
 * 회원 탈퇴는 Play 계정삭제 정책(§11) 충족.
 */
@Composable
fun MyScreen(
    onLoggedOut: () -> Unit,
    onOpenProfileEdit: () -> Unit,
    onOpenLinkedAccounts: () -> Unit,
    onOpenInquiries: () -> Unit,
    onOpenMyMemos: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deleting by viewModel.deleting.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 프로필 수정/연동 관리 등에서 돌아오면 내 정보를 새로고침한다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                MyUiEvent.AccountDeleted -> onLoggedOut()
                is MyUiEvent.Message -> Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    ResourceContent(resource = state, onRetry = viewModel::load, modifier = modifier) { me ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            ProfileCard(me = me)
            Spacer(Modifier.height(16.dp))

            NavRow(title = stringResource(R.string.my_nav_profile_edit), enabled = !deleting, onClick = onOpenProfileEdit)
            NavRow(title = stringResource(R.string.my_nav_linked_accounts), enabled = !deleting, onClick = onOpenLinkedAccounts)
            NavRow(title = stringResource(R.string.my_nav_my_memos), enabled = !deleting, onClick = onOpenMyMemos)
            NavRow(title = stringResource(R.string.my_nav_inquiries), enabled = !deleting, onClick = onOpenInquiries)

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onLoggedOut,
                enabled = !deleting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Text(stringResource(R.string.my_logout))
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { showDeleteDialog = true },
                enabled = !deleting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.my_withdraw), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.my_withdraw_dialog_title)) },
            text = { Text(stringResource(R.string.my_withdraw_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteAccount()
                }) {
                    Text(stringResource(R.string.my_withdraw_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun NavRow(title: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        androidx.compose.material3.Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileCard(me: AuthMeResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!me.profileImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = me.profileImageUrl,
                        contentDescription = stringResource(R.string.my_profile_image_desc),
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                    )
                    Spacer(Modifier.width(16.dp))
                }
                Column {
                    Text(
                        text = me.nickname.ifBlank { stringResource(R.string.my_nickname_fallback) },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = me.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (me.provider.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = providerLabel(me.provider),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
            }
        }
    }
}
