package com.elseeker.android.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elseeker.android.R

/** 로딩/오류/성공 분기를 표준화. 오류 시 재시도 버튼을 노출한다. */
@Composable
fun <T> ResourceContent(
    resource: UiResource<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    when (resource) {
        is UiResource.Loading -> LoadingBox(modifier)
        is UiResource.Error -> ErrorBox(resource.message, onRetry, modifier, resource.isNetwork)
        is UiResource.Success -> content(resource.data)
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorBox(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    isNetwork: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (isNetwork) "📡" else "⚠️",
            style = MaterialTheme.typography.displaySmall,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}
