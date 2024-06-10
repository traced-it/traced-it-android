package app.traced_it.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.traced_it.BuildConfig
import app.traced_it.R
import app.traced_it.ui.components.TracedScaffold
import app.traced_it.ui.components.TracedTopAppBar
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Light
import app.traced_it.ui.theme.Spacing

@Composable
fun AboutScreen(
    onNavigateToEntries: () -> Unit = {},
) {
    TracedScaffold(
        topBar = {
            TracedTopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToEntries) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val appName = stringResource(R.string.app_name)
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .padding(horizontal = Spacing.windowPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.about_app_icon_content_description),
                modifier = Modifier
                    .size(192.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Text(
                appName,
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                AnnotatedString.fromHtml(
                    stringResource(
                        R.string.about_text,
                        appName,
                        BuildConfig.VERSION_NAME
                    ),
                    linkStyles = TextLinkStyles(
                        SpanStyle(
                            color = Light,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                ),
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineBreak = LineBreak.Paragraph,
                )
            )
        }
    }
}

// Previews

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DefaultPreview() {
    AppTheme {
        AboutScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun LightPreview() {
    AppTheme {
        AboutScreen()
    }
}
