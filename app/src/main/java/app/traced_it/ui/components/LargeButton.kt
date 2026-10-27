package app.traced_it.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Spacing

@Composable
fun LargeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
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

// Previews

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DefaultPreview() {
    AppTheme {
        LargeButton(text = "Enabled button", onClick = {})
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DisabledPreview() {
    AppTheme {
        LargeButton(text = "Disabled button", onClick = {}, enabled = false)
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.TABLET)
@Composable
private fun TabletPreview() {
    AppTheme {
        LargeButton(text = "Portrait button", onClick = {})
    }
}
