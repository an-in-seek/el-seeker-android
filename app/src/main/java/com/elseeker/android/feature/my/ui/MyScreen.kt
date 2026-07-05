package com.elseeker.android.feature.my.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.elseeker.android.R
import com.elseeker.android.core.ui.ForceBlackNavigationBarInDialog
import com.elseeker.android.core.ui.ResourceContent
import com.elseeker.android.feature.auth.data.AuthMeResponse
import com.elseeker.android.feature.auth.data.MemberOAuthAccountResponse

private const val NICKNAME_MAX = 50

/**
 * 마이 탭(웹 mypage 단일 페이지 파리티): 프로필 카드(아바타·배지) → 내 정보 수정(닉네임 인라인)
 * → 연동 계정(제공사 카드·해제) → 더보기(내 메모·1:1 문의) → 로그아웃 → Danger Zone(회원 탈퇴).
 * 회원 탈퇴는 Play 계정삭제 정책(§11) 충족.
 */
@Composable
fun MyScreen(
    onLoggedOut: () -> Unit,
    onOpenInquiries: () -> Unit,
    onOpenMyMemos: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val nickname by viewModel.nickname.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val working by viewModel.working.collectAsStateWithLifecycle()
    val deleting by viewModel.deleting.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 다른 화면(내 메모/1:1 문의 등)에서 돌아오면 내 정보를 새로고침한다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                MyUiEvent.AccountDeleted -> onLoggedOut()
                MyUiEvent.ProfileSaved ->
                    Toast.makeText(context, context.getString(R.string.my_profile_edit_saved), Toast.LENGTH_SHORT).show()
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
            Text(
                text = stringResource(R.string.my_page_title),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            ProfileCard(me = me, linkedCount = accounts.size)
            Spacer(Modifier.height(16.dp))

            NicknameEditCard(
                nickname = nickname,
                saving = saving,
                onNicknameChange = viewModel::onNicknameChange,
                onSave = viewModel::saveNickname,
            )

            if (accounts.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.my_section_linked_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))
                accounts.forEach { account ->
                    LinkedAccountCard(
                        account = account,
                        isPrimary = account.provider.equals(me.provider, ignoreCase = true),
                        enabled = !working && !deleting,
                        onUnlink = { viewModel.unlink(account) },
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    NavRow(
                        title = stringResource(R.string.my_nav_my_memos),
                        enabled = !deleting,
                        onClick = onOpenMyMemos,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NavRow(
                        title = stringResource(R.string.my_nav_inquiries),
                        enabled = !deleting,
                        onClick = onOpenInquiries,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onLoggedOut,
                enabled = !deleting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.my_logout))
            }

            Spacer(Modifier.height(24.dp))
            DangerZoneCard(enabled = !deleting, onWithdraw = { showDeleteDialog = true })
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { ForceBlackNavigationBarInDialog(); Text(stringResource(R.string.my_withdraw_dialog_title)) },
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

/** 프로필 카드 — 가운데 정렬 아바타 + 닉네임 + 이메일 + 배지(권한/연동수/가입월). 웹 파리티. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileCard(me: AuthMeResponse, linkedCount: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (!me.profileImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = me.profileImageUrl,
                        contentDescription = stringResource(R.string.my_profile_image_desc),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = stringResource(R.string.my_profile_image_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(52.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = me.nickname.ifBlank { stringResource(R.string.my_nickname_fallback) },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = me.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ProfileBadge(
                    text = roleLabel(me.role),
                    container = MaterialTheme.colorScheme.primaryContainer,
                    content = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                ProfileBadge(
                    text = stringResource(R.string.my_badge_linked_count, linkedCount),
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                joinMonth(me.createdAt)?.let { (year, month) ->
                    ProfileBadge(
                        text = stringResource(R.string.my_badge_join_month, year, month),
                        container = MaterialTheme.colorScheme.surfaceVariant,
                        content = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileBadge(text: String, container: Color, content: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = content,
        )
    }
}

/** 내 정보 수정 카드 — 닉네임 인라인 편집 + 저장하기 + 글자수(N/50). 웹 파리티. */
@Composable
private fun NicknameEditCard(
    nickname: String,
    saving: Boolean,
    onNicknameChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.my_section_edit_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.my_profile_edit_nickname_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !saving,
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onSave,
                enabled = !saving && nickname.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.my_nickname_save))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.my_nickname_counter, nickname.length, NICKNAME_MAX),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
    }
}

/** 연동 계정 카드 — 제공사 아이콘·이름·연동됨 배지 + 이메일/닉네임/연동일 + 연동 해제. 웹 파리티. */
@Composable
private fun LinkedAccountCard(
    account: MemberOAuthAccountResponse,
    isPrimary: Boolean,
    enabled: Boolean,
    onUnlink: () -> Unit,
) {
    val brand = providerBrandName(account.provider)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                providerIconRes(account.provider)?.let { iconRes ->
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = brand,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(8.dp))
                LinkedBadge()
            }
            Spacer(Modifier.height(12.dp))
            AccountInfoRow(
                label = stringResource(R.string.my_account_email_label),
                value = account.email?.takeIf { it.isNotBlank() } ?: "-",
            )
            AccountInfoRow(
                label = stringResource(R.string.my_account_nickname_label),
                value = account.nickname?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.my_account_nickname_empty),
            )
            AccountInfoRow(
                label = stringResource(R.string.my_account_linked_date_label),
                value = formatLinkedDate(account.createdAt) ?: "-",
            )
            Spacer(Modifier.height(12.dp))
            if (isPrimary) {
                Text(
                    text = stringResource(R.string.my_account_primary_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.my_account_unlink_action, brand))
                }
            } else {
                OutlinedButton(
                    onClick = onUnlink,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (enabled) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
                ) {
                    Text(stringResource(R.string.my_account_unlink_action, brand))
                }
            }
        }
    }
}

@Composable
private fun LinkedBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFDCFCE7))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = stringResource(R.string.my_account_linked_badge),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF15803D),
        )
    }
}

@Composable
private fun AccountInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.width(64.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Danger Zone 카드 — 회원 탈퇴 안내 + 버튼(붉은 강조). 웹 파리티. */
@Composable
private fun DangerZoneCard(enabled: Boolean, onWithdraw: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.my_danger_zone_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.my_danger_zone_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onWithdraw,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.my_withdraw_action))
            }
        }
    }
}

@Composable
private fun NavRow(title: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 권한 코드 → 한국어 라벨. */
@Composable
private fun roleLabel(role: String): String = when (role.uppercase()) {
    "ADMIN" -> stringResource(R.string.my_role_admin)
    else -> stringResource(R.string.my_role_member)
}

/** createdAt(ISO-8601, "YYYY-MM-DD…") → (연, 월). 파싱 불가 시 null. */
private fun joinMonth(createdAt: String?): Pair<Int, Int>? {
    val s = createdAt ?: return null
    if (s.length < 7) return null
    val year = s.substring(0, 4).toIntOrNull() ?: return null
    val month = s.substring(5, 7).toIntOrNull() ?: return null
    return year to month
}

/** createdAt → "YYYY. MM. DD." 표기. 파싱 불가 시 null. */
private fun formatLinkedDate(createdAt: String?): String? {
    val s = createdAt ?: return null
    if (s.length < 10) return null
    return "${s.substring(0, 4)}. ${s.substring(5, 7)}. ${s.substring(8, 10)}."
}
