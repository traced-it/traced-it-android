package app.traced_it.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Spacing

@Composable
fun TracedBottomButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Button(
            onClick = onClick,
            modifier = modifier
                .padding(Spacing.small)
                .width(400.dp)
                .height(Spacing.bottomButtonHeight),
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                disabledContainerColor = MaterialTheme.colorScheme.secondary,
                disabledContentColor = MaterialTheme.colorScheme.onSecondary
            ),
        ) {
            Text(text, fontWeight = FontWeight.Bold)
        }
    }
}

// Previews

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DefaultPreview() {
    AppTheme {
        TracedBottomButton("Enabled button", {})
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DisabledPreview() {
    AppTheme {
        TracedBottomButton("Disabled button", {}, enabled = false)
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 480
)
@Composable
private fun PortraitPreview() {
    AppTheme {
        TracedBottomButton("Portrait button", {})
    }
}
